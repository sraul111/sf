Tool: parts-stock-mcp (Python stdio MCP server)
===============================================

This file defines strict JSON contracts and examples for the three MCP tools the assistant may call. The server is a Python stdio MCP server that validates JSON input against the schemas below and replies with JSON responses on stdout, one JSON object per line.

General request envelope
------------------------
Every request is a JSON object with these top-level keys:

{
  "tool": "<tool_name>",
  "args": { ... },
  "meta": {
    "session_token": "<optional session token>",
    "idempotency_key": "<optional idempotency key>",
    "caller": "assistant"
  }
}

General success response
------------------------
{
  "ok": true,
  "result": { ... }
}

General error response (see error contract in tool-design.md)
----------------------------------------------------------
{
  "ok": false,
  "status": 400,
  "code": "invalid_input",
  "message": "...",
  "details": { }
}

1) get_technician_availability
-------------------------------
Purpose: authoritative availability check for a technician against the 45-minute travel buffer.

Request schema (args):

{
  "type": "object",
  "required": ["technicianId", "jobStart", "jobEnd"],
  "properties": {
    "technicianId": {"type": "integer", "minimum": 1},
    "jobStart": {"type": "string", "format": "date-time"},
    "jobEnd": {"type": "string", "format": "date-time"},
    "require_no_conflict": {"type": "boolean"}
  }
}

Semantics:
- `jobStart` and `jobEnd` are ISO-8601 datetimes interpreted as UTC.
- The server computes the buffered interval W = [jobStart - 45min, jobEnd + 45min] inclusive.
- It scans current jobs for the technician and returns any conflicts.
- If `require_no_conflict` is true and any conflict exists, the tool returns a 409 with `conflict` code and details of the conflicting job ids and overlapping windows.

Success result:

{
  "ok": true,
  "result": {
    "technicianId": 1,
    "requestedWindow": {"start":"2026-09-30T10:00:00Z","end":"2026-09-30T12:00:00Z"},
    "bufferedWindow": {"start":"2026-09-30T09:15:00Z","end":"2026-09-30T12:45:00Z"},
    "conflicts": [ {"jobId": 12, "jobWindow": {"start":"...","end":"..."} } ],
    "available": false
  }
}

Error examples:
- 400 invalid datetime: status 400, code `invalid_input`, details.field = `jobStart`
- 404 technician missing: status 404, code `not_found`
- 409 conflicts: status 409, code `conflict`, details.conflicts = [ ... ]

2) get_part_stock
------------------
Purpose: read-only stock projection for a SKU at now (UTC) and optionally peak during a candidate window.

Request schema (args):

{
  "type": "object",
  "required": ["sku"],
  "properties": {
    "sku": {"type": "string"},
    "windowStart": {"type": "string", "format": "date-time"},
    "windowEnd": {"type": "string", "format": "date-time"},
    "include_peak": {"type": "boolean"}
  }
}

Semantics:
- `sku` matched case-insensitively to the seeded catalog at `serviceforge://catalog/parts`.
- If `include_peak` is true and a window is provided, the tool computes the peak reserved quantity across the buffered job windows intersecting the provided window and returns `peakReserved` and `peakAvailable = total - peakReserved`.

Success result:

{
  "ok": true,
  "result": {
    "sku": "CMP-CLUTCH",
    "total": 3,
    "inUseNow": 1,
    "availableNow": 2,
    "peakReserved": 2,
    "peakAvailable": 1
  }
}

Error examples:
- 404 sku missing: status 404, code `not_found`
- 400 bad datetime: status 400, code `invalid_input`

3) reserve_part_for_job
------------------------
Purpose: guarded write to create a reservation (issue) of `quantity` of `sku` for `jobId`.

Request schema (args):

{
  "type": "object",
  "required": ["jobId","sku","quantity"],
  "properties": {
    "jobId": {"type": "integer", "minimum": 1},
    "sku": {"type": "string"},
    "quantity": {"type": "integer", "minimum": 1},
    "session_brief_ref": {"type": "string"}
  }
}

Semantics & preconditions:
- `meta.session_token` MUST be supplied and correspond to `serviceforge://session/dispatch-brief` ownership that includes the same `jobId` and the requestor as the technician; otherwise return 403.
- The referenced job must exist and be `SCHEDULED` (otherwise 409).
- SKU is resolved case-insensitively; not found → 404.
- Quantity > available across the job's buffered window → 409 with details: {total, requested, peakReserved}.
- The tool acquires the inventory lock, validates again, writes the issue record, emits an audit event, and returns a reservation id. If the idempotency_key matches an earlier identical request, return the original result (idempotent).

Success result:

{
  "ok": true,
  "result": {
    "reservationId": "resv-20260929-0001",
    "jobId": 12,
    "sku": "CMP-CLUTCH",
    "quantity": 1
  }
}

Error examples:
- 400 invalid quantity → `invalid_input`
- 403 wrong owner / brief mismatch → `forbidden`
- 404 sku or job not found → `not_found`
- 409 insufficient stock → `conflict` with details { total, requested, peakReserved }

Resource examples
-----------------
- serviceforge://catalog/parts
  - JSON array of parts with {id, sku, name, total}

- serviceforge://session/dispatch-brief
  - {
      "sessionId": "s-abc",
      "technicianId": 1,
      "jobId": 12,
      "requestedSku": "CMP-CLUTCH",
      "requestedQuantity": 1,
      "requestorId": "user-42",
      "humanConfirmed": false
    }

Security boundaries
-------------------
- The MCP server must refuse reserve requests coming from untrusted contexts; a session token binds the brief to a session and an authenticated human. The tool must validate this binding.
- The server may log all request meta but must redact `meta.session_token` in logs.
- No tool allows editing job times or creating/deleting technicians.

Implementation note (Python)
---------------------------
- Use `jsonschema` for request validation, `pydantic` for typed models optionally, and `filelock` or a simple threading.Lock around inventory mutations.
- Accept and return newline-delimited JSON on stdin/stdout. Runloop must catch, log, and return structured errors for any unexpected internal exception (500 with code `internal_error`).
