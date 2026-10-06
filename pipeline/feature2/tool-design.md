Feature 2 — MCP Tool Design (overview)

Purpose
-------
This document defines the architecture and runtime contract for a small, Python-based stdio MCP (Model-Controller-Protocol) server that exposes precisely three tools used by an assistant to safely check availability and parts stock and to reserve parts for a job as part of a prep-job-dispatch workflow.

Goals
-----
- Keep the assistant "read-first, ask-human-last": tools answer factual questions and execute a guarded write only after a human confirmation step.
- Enforce domain rules in the tools (45-minute travel buffer, UTC time math, SKU case-insensitive lookup, job ownership) so a model cannot bypass business logic.
- Provide strict, machine-checked JSON contracts and deterministic error shapes for easy client validation and testability.
- Prevent arbitrary state changes by limiting write surface, requiring pre-authorized session briefs, idempotency tokens, and service-account scoped credentials.

Runtime model
-------------
- Implementation: Python 3.10+ stdio server (single-process) that reads newline-delimited JSON requests from stdin and writes newline-delimited JSON responses to stdout.
- Each incoming line is a single JSON object: {"tool": "<tool_name>", "args": { ... }, "meta": {...} }
- Each response is a single JSON object with one of two shapes: success or error (see Error contract below).
- Server MUST validate every request against the tool's JSON schema before executing logic and MUST never execute unvalidated inputs.
- The server runs with a scoped service account and authenticates callers using a per-session short-lived token presented in `meta.session_token`.

Resource URIs
-------------
- `serviceforge://catalog/parts` — authoritative seeded catalog (read-only to assistant; developer code holds totals and availability in MockDataStore). The MCP server may read projection views from this resource.
- `serviceforge://session/dispatch-brief` — the ephemeral session record the assistant assembled about the pending dispatch. Contains technicianId, jobId, requestedSku (optional), requestedQuantity (optional), requestorId, and humanConfirmationState. The MCP server must treat this as authoritative for ownership checks when `meta.session_token` is presented.

Tools (summary)
----------------
1. `get_technician_availability` — read-only. Enforces 45-minute travel buffer when reporting availability windows for a technician.
2. `get_part_stock` — read-only. Returns current availability projection for a SKU at the server's UTC 'now' and peak-window availability when requested.
3. `reserve_part_for_job` — write. Creates an allocation/issue record for a job (requires session brief and human confirmation). Performs full validation, acquires inventory lock, and returns a reservation id on success.

Error contract (strict)
----------------------
All error responses return HTTP-style `status` integers and a machine-readable `code` along with a human `message` and optional `details` object:

{
  "ok": false,
  "status": 400,
  "code": "invalid_input",
  "message": "Quantity must be a positive integer",
  "details": { "field": "quantity" }
}

Status → code mapping (tool-level semantics):
- 400 → `invalid_input` (schema/validation failures, non-positive quantities, return > outstanding etc.)
- 403 → `forbidden` (attempt against another technician's job, invalid session brief ownership)
- 404 → `not_found` (sku not present, job id missing)
- 409 → `conflict` (insufficient stock across the buffered window; job not SCHEDULED)

Security & safety
-----------------
- The MCP server only accepts requests that include `meta.session_token` referencing a valid session; read-only tools may optionally be allowed without a session token for public catalog reads, but `reserve_part_for_job` always requires a session token and `dispatch-brief` to confirm ownership.
- Writes are idempotent: `reserve_part_for_job` accepts `meta.idempotency_key` and returns the same reservation id for identical calls.
- The server must never accept arbitrary state-changing requests (no endpoints that edit job times or technician records). The only mutating tool is `reserve_part_for_job`, scoped to creating an issue/reservation record.
- All time math is UTC; the server uses `datetime.now(timezone.utc)` and expects job datetimes to be normalized to UTC.

Prompt workflow: `prep-job-dispatch`
----------------------------------
This workflow is the recommended assistant prompt flow (model → tools → human):

1. Assistant collects or reads `serviceforge://session/dispatch-brief` (brief MUST include technicianId and jobId).
2. Assistant calls `get_technician_availability` with `technicianId` and job window to ensure the technician has no booking conflicts (the tool enforces 45-minute travel buffer — its result is authoritative).
3. Assistant calls `get_part_stock` with `sku` (if provided) to check current and windowed availability.
4. If availability and stock checks pass, assistant generates a short human-facing summary with the three key facts (brief, technician availability verdict, stock verdict) and asks the human to confirm reserve.
5. On human explicit confirmation, assistant calls `reserve_part_for_job` with `jobId`, `sku`, `quantity`, `meta.session_token`, and `meta.idempotency_key`. The tool performs final validation and returns reservation id or an error. Assistant surfaces the result to the human.

Audit and observability
-----------------------
- Every write (reserve_part_for_job) emits a structured audit event to the process log and the `serviceforge://session/dispatch-brief` event list: {timestampUTC, actor, jobId, sku, quantity, reservationId, result}.
- Rate-limit of writes and a small cooldown (500ms) inside reserve to reduce contention for last-unit races is recommended; the server must still use the inventory lock for correctness.

Testing guidance
----------------
- Unit-test the MCP server by sending deterministic JSON lines to stdin and capturing stdout.
- Provide fixtures for `serviceforge://catalog/parts` containing the four seeded SKUs.
- Use a fixed `Clock` for tests (UTC) and include concurrent test that issues two simultaneous reserve requests for the last unit; expect exactly one success (201/ok) and one 409.

Files: see `pipeline/feature2/tools/parts-stock-tool.md` for the full tool contracts, schemas, and examples.
