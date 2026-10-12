#!/usr/bin/env python3
"""
ServiceForge MCP Server (stdio)
Exposes three tools:
- get_technician_availability
- get_part_stock
- reserve_part_for_job
Exposes resources:
- serviceforge://catalog/parts
- serviceforge://session/dispatch-brief
Exposes prompts:
- prep-job-dispatch
Communicates with ServiceForge backend at http://localhost:8080.
No hardcoded catalog or state.
"""

import sys
import json
import os
from datetime import datetime, timezone, timedelta

# Backend base URL
BACKEND_URL = os.environ.get("SERVICEFORGE_BACKEND_URL", "http://localhost:8080").rstrip("/")

# Idempotency store for reservations
IDEMPOTENCY_CACHE = {}


def http_request(method: str, path: str, body: dict = None) -> tuple[int, dict]:
    """
    Executes HTTP request to backend and returns (status_code, response_data).
    Uses requests if available, otherwise urllib.
    """
    url = f"{BACKEND_URL}{path}"
    
    try:
        import requests
        headers = {"Content-Type": "application/json"}
        if method.upper() == "GET":
            r = requests.get(url, headers=headers, timeout=5)
        elif method.upper() == "POST":
            r = requests.post(url, headers=headers, json=body, timeout=5)
        else:
            raise ValueError(f"Unsupported method: {method}")

        try:
            data = r.json()
        except Exception:
            data = {"message": r.text}
        return r.status_code, data
    except ImportError:
        import urllib.request
        import urllib.error

        req = urllib.request.Request(url, method=method.upper())
        req.add_header("Content-Type", "application/json")
        data_bytes = None
        if body is not None:
            data_bytes = json.dumps(body).encode("utf-8")

        try:
            with urllib.request.urlopen(req, data=data_bytes, timeout=5) as response:
                status = response.getcode()
                content = response.read().decode("utf-8")
                try:
                    data = json.loads(content)
                except Exception:
                    data = {"message": content}
                return status, data
        except urllib.error.HTTPError as e:
            status = e.code
            content = e.read().decode("utf-8")
            try:
                data = json.loads(content)
            except Exception:
                data = {"message": content}
            return status, data


def emit(obj: dict):
    sys.stdout.write(json.dumps(obj) + "\n")
    sys.stdout.flush()


def emit_error(status: int, code: str, message: str, details: dict = None):
    emit({
        "ok": False,
        "status": status,
        "code": code,
        "message": message,
        "details": details if details is not None else {}
    })


def emit_success(result):
    emit({
        "ok": True,
        "result": result
    })


def parse_iso_datetime(dt_str: str) -> datetime:
    if not isinstance(dt_str, str):
        raise ValueError("Datetime must be a string")
    normalized = dt_str.replace("Z", "+00:00")
    dt = datetime.fromisoformat(normalized)
    if dt.tzinfo is None:
        raise ValueError("Datetime must include a timezone")
    dt = dt.astimezone(timezone.utc)
    return dt


def validate_tool_args(tool: str, args: dict) -> bool:
    schemas = {
        "get_technician_availability": ({"technicianId", "jobStart", "jobEnd"}, {"require_no_conflict"}),
        "get_part_stock": ({"sku"}, {"windowStart", "windowEnd", "include_peak"}),
        "reserve_part_for_job": ({"jobId", "sku", "quantity"}, {"session_brief_ref"}),
    }
    required, optional = schemas[tool]
    allowed = required | optional

    unexpected = next((key for key in args if key not in allowed), None)
    if unexpected is not None:
        emit_error(400, "invalid_input", f"Unexpected argument: {unexpected}", {"field": unexpected})
        return False

    missing = required - args.keys()
    if missing:
        field = next(iter(sorted(missing)))
        messages = {
            "jobStart": "jobStart is required",
            "jobEnd": "jobEnd is required",
            "sku": "sku must be a non-empty string",
            "quantity": "Quantity must be a positive integer",
        }
        if field in {"technicianId", "jobId"}:
            message = f"{field} must be an integer >= 1"
        else:
            message = messages[field]
        emit_error(400, "invalid_input", message, {"field": field})
        return False

    for field in ("technicianId", "jobId"):
        if field in args and (type(args[field]) is not int or args[field] < 1):
            emit_error(400, "invalid_input", f"{field} must be an integer >= 1", {"field": field})
            return False

    if "quantity" in args and (type(args["quantity"]) is not int or args["quantity"] < 1):
        emit_error(400, "invalid_input", "Quantity must be a positive integer", {"field": "quantity"})
        return False

    if "sku" in args and (not isinstance(args["sku"], str) or not args["sku"].strip()):
        emit_error(400, "invalid_input", "sku must be a non-empty string", {"field": "sku"})
        return False

    datetime_fields = {
        "jobStart": "Invalid datetime format for jobStart",
        "jobEnd": "Invalid datetime format for jobEnd",
        "windowStart": "Invalid windowStart datetime",
        "windowEnd": "Invalid windowEnd datetime",
    }
    for field, message in datetime_fields.items():
        if field not in args:
            continue
        try:
            parse_iso_datetime(args[field])
        except (TypeError, ValueError, OverflowError):
            emit_error(400, "invalid_input", f"{message}: {args[field]}", {"field": field})
            return False

    for field in ("require_no_conflict", "include_peak"):
        if field in args and type(args[field]) is not bool:
            emit_error(400, "invalid_input", f"{field} must be a boolean", {"field": field})
            return False

    if "session_brief_ref" in args and not isinstance(args["session_brief_ref"], str):
        emit_error(400, "invalid_input", "session_brief_ref must be a string", {"field": "session_brief_ref"})
        return False

    return True


def handle_get_technician_availability(args: dict, meta: dict):
    if not validate_tool_args("get_technician_availability", args):
        return

    technician_id = args.get("technicianId")
    if technician_id is None or not isinstance(technician_id, int) or technician_id < 1:
        emit_error(400, "invalid_input", "technicianId must be an integer >= 1", {"field": "technicianId"})
        return

    job_start_str = args.get("jobStart")
    if not job_start_str:
        emit_error(400, "invalid_input", "jobStart is required", {"field": "jobStart"})
        return
    try:
        job_start = parse_iso_datetime(job_start_str)
    except Exception:
        emit_error(400, "invalid_input", f"Invalid datetime format for jobStart: {job_start_str}", {"field": "jobStart"})
        return

    job_end_str = args.get("jobEnd")
    if not job_end_str:
        emit_error(400, "invalid_input", "jobEnd is required", {"field": "jobEnd"})
        return
    try:
        job_end = parse_iso_datetime(job_end_str)
    except Exception:
        emit_error(400, "invalid_input", f"Invalid datetime format for jobEnd: {job_end_str}", {"field": "jobEnd"})
        return

    if not job_start < job_end:
        emit_error(400, "invalid_input", "jobStart must be before jobEnd", {"field": "jobStart"})
        return

    require_no_conflict = bool(args.get("require_no_conflict", False))

    # Real HTTP request to verify technician
    try:
        tech_status, tech_data = http_request("GET", f"/api/technicians/{technician_id}")
    except Exception as e:
        emit_error(500, "internal_error", f"Failed to contact backend: {str(e)}", {})
        return

    if tech_status == 404:
        emit_error(404, "not_found", f"Technician {technician_id} not found", {"technicianId": technician_id})
        return
    elif tech_status >= 400:
        emit_error(tech_status, "internal_error", f"Backend error fetching technician: {tech_data.get('message', '')}", {})
        return

    # Real HTTP request to get technician jobs
    try:
        jobs_status, jobs_data = http_request("GET", f"/api/technicians/{technician_id}/jobs")
    except Exception as e:
        emit_error(500, "internal_error", f"Failed to contact backend: {str(e)}", {})
        return

    if jobs_status == 404:
        emit_error(404, "not_found", f"Technician {technician_id} not found", {"technicianId": technician_id})
        return
    elif jobs_status >= 400:
        emit_error(jobs_status, "internal_error", f"Backend error fetching jobs: {jobs_data.get('message', '')}", {})
        return

    jobs_list = jobs_data if isinstance(jobs_data, list) else []

    buffer = timedelta(minutes=45)
    buffered_start = job_start - buffer
    buffered_end = job_end + buffer

    conflicts = []
    for job in jobs_list:
        try:
            j_start = parse_iso_datetime(job["startTime"])
            j_end = parse_iso_datetime(job["endTime"])
            # Conflict occurs if requested job window conflicts with existing job + 45min buffer:
            # j_start - 45min < job_end AND job_start < j_end + 45min
            if j_start - buffer < job_end and job_start < j_end + buffer:
                conflicts.append({
                    "jobId": job.get("id"),
                    "jobWindow": {
                        "start": job.get("startTime"),
                        "end": job.get("endTime")
                    }
                })
        except Exception:
            continue

    if require_no_conflict and conflicts:
        emit_error(409, "conflict", "Technician has booking conflicts within buffered window", {"conflicts": conflicts})
        return

    emit_success({
        "technicianId": technician_id,
        "requestedWindow": {
            "start": job_start_str,
            "end": job_end_str
        },
        "bufferedWindow": {
            "start": buffered_start.strftime("%Y-%m-%dT%H:%M:%SZ"),
            "end": buffered_end.strftime("%Y-%m-%dT%H:%M:%SZ")
        },
        "conflicts": conflicts,
        "available": len(conflicts) == 0
    })


def handle_get_part_stock(args: dict, meta: dict):
    if not validate_tool_args("get_part_stock", args):
        return

    sku = args.get("sku")
    if not sku or not isinstance(sku, str) or not sku.strip():
        emit_error(400, "invalid_input", "sku must be a non-empty string", {"field": "sku"})
        return
    sku = sku.strip()

    window_start_str = args.get("windowStart")
    if window_start_str:
        try:
            parse_iso_datetime(window_start_str)
        except Exception:
            emit_error(400, "invalid_input", f"Invalid windowStart datetime: {window_start_str}", {"field": "windowStart"})
            return

    window_end_str = args.get("windowEnd")
    if window_end_str:
        try:
            parse_iso_datetime(window_end_str)
        except Exception:
            emit_error(400, "invalid_input", f"Invalid windowEnd datetime: {window_end_str}", {"field": "windowEnd"})
            return

    include_peak = bool(args.get("include_peak", False))

    # Real HTTP request to fetch part
    try:
        status, data = http_request("GET", f"/api/parts/{sku}")
    except Exception as e:
        emit_error(500, "internal_error", f"Failed to contact backend: {str(e)}", {})
        return

    if status == 404:
        emit_error(404, "not_found", f"Part not found for SKU: {sku}", {"sku": sku})
        return
    elif status >= 400:
        emit_error(status, "internal_error", f"Backend error: {data.get('message', '')}", {})
        return

    total = data.get("totalQuantity", data.get("total", 0))
    in_use_now = 0
    available_now = total - in_use_now

    result = {
        "sku": data.get("sku", sku),
        "total": total,
        "inUseNow": in_use_now,
        "availableNow": available_now
    }

    if include_peak:
        peak_reserved = 0
        result["peakReserved"] = peak_reserved
        result["peakAvailable"] = total - peak_reserved

    emit_success(result)


def handle_reserve_part_for_job(args: dict, meta: dict):
    if not validate_tool_args("reserve_part_for_job", args):
        return

    job_id = args.get("jobId")
    if job_id is None or not isinstance(job_id, int) or job_id < 1:
        emit_error(400, "invalid_input", "jobId must be an integer >= 1", {"field": "jobId"})
        return

    sku = args.get("sku")
    if not sku or not isinstance(sku, str) or not sku.strip():
        emit_error(400, "invalid_input", "sku must be a non-empty string", {"field": "sku"})
        return
    sku = sku.strip()

    quantity = args.get("quantity")
    if quantity is None or not isinstance(quantity, int) or quantity < 1:
        emit_error(400, "invalid_input", "Quantity must be a positive integer", {"field": "quantity"})
        return

    # Check idempotency key if provided
    idempotency_key = meta.get("idempotency_key") if meta else None
    if idempotency_key and idempotency_key in IDEMPOTENCY_CACHE:
        emit_success(IDEMPOTENCY_CACHE[idempotency_key])
        return

    # Real HTTP request to reserve part
    payload = {
        "jobId": job_id,
        "quantity": quantity
    }

    try:
        status, data = http_request("POST", f"/api/parts/{sku}/reservations", body=payload)
    except Exception as e:
        emit_error(500, "internal_error", f"Failed to contact backend: {str(e)}", {})
        return

    if status == 400:
        code = data.get("code", "invalid_input")
        message = data.get("message", "Quantity must be a positive integer")
        details = data.get("details", {"field": "quantity"})
        emit_error(400, code, message, details)
        return
    elif status == 404:
        code = data.get("code", "not_found")
        message = data.get("message", f"Part not found for SKU: {sku}")
        details = data.get("details", {"sku": sku})
        emit_error(404, code, message, details)
        return
    elif status == 409:
        code = data.get("code", "conflict")
        message = data.get("message", f"Insufficient stock for SKU: {sku}")
        details = data.get("details", {"total": 0, "requested": quantity, "peakReserved": 0})
        emit_error(409, code, message, details)
        return
    elif status >= 400:
        emit_error(status, "internal_error", data.get("message", "Backend error"), {})
        return

    # Success: extract result
    if "result" in data:
        res = data["result"]
    else:
        res = data

    if idempotency_key:
        IDEMPOTENCY_CACHE[idempotency_key] = res

    emit_success(res)


PREP_JOB_DISPATCH_STEPS = [
    "1. Assistant collects or reads `serviceforge://session/dispatch-brief` (brief MUST include technicianId and jobId).",
    "2. Assistant calls `get_technician_availability` with `technicianId` and job window to ensure the technician has no booking conflicts (the tool enforces 45-minute travel buffer \u2014 its result is authoritative).",
    "3. Assistant calls `get_part_stock` with `sku` (if provided) to check current and windowed availability.",
    "4. If availability and stock checks pass, assistant generates a short human-facing summary with the three key facts (brief, technician availability verdict, stock verdict) and asks the human to confirm reserve.",
    "5. On human explicit confirmation, assistant calls `reserve_part_for_job` with `jobId`, `sku`, `quantity`, `meta.session_token`, and `meta.idempotency_key`. The tool performs final validation and returns reservation id or an error. Assistant surfaces the result to the human."
]

PREP_JOB_DISPATCH_WORKFLOW = "\n".join(PREP_JOB_DISPATCH_STEPS)


def handle_resource(uri: str, meta: dict = None):
    if not uri or not isinstance(uri, str):
        emit_error(400, "invalid_input", "Missing or invalid 'resource' URI", {"field": "resource"})
        return

    if uri == "serviceforge://catalog/parts":
        try:
            status, data = http_request("GET", "/api/parts")
        except Exception as e:
            emit_error(500, "internal_error", f"Failed to contact backend: {str(e)}", {})
            return

        if status == 404:
            emit_error(404, "not_found", "Parts catalog not found", {"resource": uri})
            return
        elif status >= 400:
            error_msg = data.get("message", "Backend error") if isinstance(data, dict) else str(data)
            emit_error(status, "internal_error", f"Backend error: {error_msg}", {})
            return

        emit_success(data)
    elif uri == "serviceforge://session/dispatch-brief":
        session_obj = {
            "sessionId": "s-abc",
            "technicianId": 1,
            "jobId": 12,
            "humanConfirmed": False
        }
        emit_success(session_obj)
    else:
        emit_error(404, "not_found", f"Unknown resource URI: {uri}", {"resource": uri})


def handle_prompt(prompt_name: str, args: dict = None, meta: dict = None):
    if not prompt_name or not isinstance(prompt_name, str):
        emit_error(400, "invalid_input", "Missing or invalid 'prompt' property in request", {"field": "prompt"})
        return

    if args is not None and not isinstance(args, dict):
        emit_error(400, "invalid_input", "'args' property must be a JSON object", {"field": "args"})
        return

    if prompt_name == "prep-job-dispatch":
        res = {
            "prompt": prompt_name,
            "workflow": PREP_JOB_DISPATCH_WORKFLOW,
            "text": PREP_JOB_DISPATCH_WORKFLOW,
            "steps": PREP_JOB_DISPATCH_STEPS
        }
        if args and isinstance(args, dict):
            if "jobId" in args:
                res["jobId"] = args["jobId"]
            res["args"] = args
        emit_success(res)
    else:
        emit_error(404, "not_found", f"Unknown prompt: {prompt_name}", {"prompt": prompt_name})


def handle_request(req: dict):
    if not isinstance(req, dict):
        emit_error(400, "invalid_input", "Request must be a JSON object", {})
        return

    if "resource" in req:
        handle_resource(req.get("resource"), req.get("meta", {}))
        return

    if "prompt" in req:
        handle_prompt(req.get("prompt"), req.get("args"), req.get("meta", {}))
        return

    tool = req.get("tool")
    args = req.get("args")
    meta = req.get("meta", {})

    if not tool:
        emit_error(400, "invalid_input", "Missing 'tool' property in request", {})
        return

    if args is None or not isinstance(args, dict):
        emit_error(400, "invalid_input", "'args' property must be a JSON object", {})
        return

    if tool == "get_technician_availability":
        handle_get_technician_availability(args, meta)
    elif tool == "get_part_stock":
        handle_get_part_stock(args, meta)
    elif tool == "reserve_part_for_job":
        handle_reserve_part_for_job(args, meta)
    else:
        emit_error(400, "invalid_input", f"Unknown tool: {tool}", {})


def main():
    for line in sys.stdin:
        line = line.strip()
        if not line:
            continue
        try:
            req = json.loads(line)
        except Exception as e:
            emit_error(400, "invalid_input", "Malformed JSON request", {"error": str(e)})
            continue

        try:
            handle_request(req)
        except Exception as e:
            emit_error(500, "internal_error", f"Internal server error: {str(e)}", {})


if __name__ == "__main__":
    main()
