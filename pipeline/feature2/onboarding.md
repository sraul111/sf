# Parts Reservation Onboarding

This guide covers the checked-in parts API and the optional Python stdio adapter. Feature 2's intended job-linked issue/return flow is not fully implemented yet; read [Current limitations and safety](#current-limitations-and-safety) before trying a write.

## Prerequisites

- Backend: Java `17` and Maven `3.9.9` are the onboarding verification baseline (the observed build used Java `17.0.20.1`); the project pins Spring Boot `3.2.5` and Java release `17` in `backend/pom.xml`. The repository does not include a Maven Wrapper or enforce the Maven patch version.
- Adapter: CPython `3.10` or newer is required; `3.10` is the minimum language/runtime contract. The repository does not pin a Python patch release or include a Python dependency lock. The adapter uses the standard library; `requests` is optional and unpinned, so do not install it as a deployment dependency without adding a reviewed version pin.
- Frontend (optional): Angular packages are declared in `frontend/package.json`, and `frontend/package-lock.json` (lockfile format 3) pins the npm dependency graph. Node.js/npm executable versions are not pinned by the repository; use an Angular 17-compatible Node.js version and `npm ci`. The frontend toolchain is optional for running the backend and adapter.

No database or external inventory service is required. The backend uses seeded in-memory data.

### Adapter Version and Wire Contract

The adapter's documented compatibility version is `v0.1.0`, matching the Feature 2 baseline in [CHANGELOG.md](CHANGELOG.md). This pins the source/API compatibility label only; it is not an independently released or version-reporting package. Use the same repository revision for adapter and backend. There is no MCP SDK dependency or MCP protocol version: despite the directory and module name, `mcp_server/server.py` implements a custom newline-delimited JSON interface, not the standard MCP JSON-RPC transport. It must not be configured as a standards-compliant MCP server without a compatible transport implementation.

Each input line is one JSON object. Tool requests have `{ "tool": "<name>", "args": {}, "meta": {} }`; resource requests have `{ "resource": "<uri>", "meta": {} }`; prompt requests have `{ "prompt": "<name>", "args": {}, "meta": {} }`. Each output line is one JSON response: success is `{ "ok": true, "result": ... }`; error is `{ "ok": false, "status": <integer>, "code": "...", "message": "...", "details": {} }`. The supported names are `get_technician_availability`, `get_part_stock`, `reserve_part_for_job`; resource URIs are `serviceforge://catalog/parts` and `serviceforge://session/dispatch-brief`; the prompt name is `prep-job-dispatch`.

The adapter's actual backend dependency is pinned to these routes and shapes:

| Backend request made by adapter | Required request/response contract |
|---|---|
| `GET /api/technicians/{id}` | Path `id` is a `Long`; `200` returns the serialized technician model; `404` returns `ApiError { "message": <string> }`. |
| `GET /api/technicians/{id}/jobs` | Path `id` is a `Long`; `200` returns an array of serialized job models with `startTime` and `endTime` date-time strings; `404` returns `ApiError { "message": <string> }`. |
| `GET /api/parts` | `200` returns `Part[]`, each `{ "id": <integer>, "sku": <string>, "name": <string>, "totalQuantity": <integer> }`; used by `serviceforge://catalog/parts`. |
| `GET /api/parts/{sku}` | `200` returns one `Part` with the same four fields; `404` returns `{ "ok": false, "status": 404, "code": "not_found", "message": <string>, "details": { "sku": <string> } }`. SKU lookup is case-insensitive in the current backend. |
| `POST /api/parts/{sku}/reservations` | JSON body `{ "jobId": <integer>, "quantity": <positive integer> }`; `200` returns `{ "ok": true, "result": { "reservationId": <string>, "jobId": <integer|null>, "sku": <string>, "quantity": <integer> } }`. Errors are `{ "ok": false, "status": <400|404|409>, "code": <string>, "message": <string>, "details": <object> }`; the adapter maps those statuses to its error envelope. |

These are the checked-in backend contracts, not the proposed future Feature 2 API. In particular, the current reservation endpoint does not enforce or persist the job relationship, and the adapter does not send authorization context. The intended `/issues` and `/returns` endpoints described in the design documents are not implemented and are not part of this compatibility contract.

## Install and Configure

From the repository root, install frontend dependencies only when using the Angular app:

```powershell
cd frontend
npm install
```

The backend has no separate install command; Maven resolves its dependencies when it runs. Backend defaults are in `backend/src/main/resources/application.properties`:

- API base URL: `http://localhost:8080`
- Angular development CORS origin: `http://localhost:4200`

To use a different backend port, set Spring's standard `SERVER_PORT` environment variable before starting the backend. When using the Python adapter against a non-default backend URL, set `SERVICEFORGE_BACKEND_URL` in the shell that launches it:

```powershell
$env:SERVICEFORGE_BACKEND_URL = 'http://localhost:8080'
```

```bash
export SERVICEFORGE_BACKEND_URL=http://localhost:8080
```

The adapter uses Python's standard library if `requests` is unavailable, so no Python package installation is required for its current implementation.

## First Run

Start the backend in one terminal:

```powershell
cd backend
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. In another PowerShell terminal, verify the seeded catalog:

```powershell
Invoke-RestMethod http://localhost:8080/api/parts
```

It should return four records in seed order: `CMP-CLUTCH` (3), `FLT-16` (10), `BRK-20A` (5), and `VLV-BALL-075` (3). The current response contains `id`, `sku`, `name`, and `totalQuantity`; it does not yet include derived `inUse` or `available` counts.

To start the Angular application, use another terminal:

```powershell
cd frontend
npm start
```

Open `http://localhost:4200`. The frontend is configured for the backend's default local URL. The parts reservation spec calls for a live parts panel, but the presence of the seeded parts API alone does not mean the complete Feature 2 UI is implemented.

## Try the Reservation Endpoint

The current API exposes `GET /api/parts`, `GET /api/parts/{sku}`, and `POST /api/parts/{sku}/reservations`. For example, this request attempts to reserve one `CMP-CLUTCH` and includes seeded job id `1`:

```powershell
$body = @{ jobId = 1; quantity = 1 } | ConvertTo-Json
Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/parts/CMP-CLUTCH/reservations' `
  -ContentType 'application/json' `
  -Body $body
```

To inspect the result, call `GET http://localhost:8080/api/parts` again. In the current implementation the `totalQuantity` itself is decreased by a successful reservation. Restarting the backend reseeds memory and restores the starting values.

Expected errors include `400` for a missing, zero, or negative quantity; `404` for an unknown SKU; and `409` when the requested quantity exceeds the current stored total. Error responses include `ok`, `status`, `code`, `message`, and `details` fields.

## Optional Python Adapter

The adapter is a local newline-delimited JSON process, not a separately installed network service. Start the backend first, then launch the process from the repository root:

```powershell
python pipeline/feature2/mcp_server/server.py
```

It reads one JSON request per line from standard input and writes one JSON response per line to standard output. For a read-only catalog-resource smoke check, enter this line and press Enter:

```json
{"resource":"serviceforge://catalog/parts"}
```

The adapter currently accepts tool requests such as `get_part_stock` and `reserve_part_for_job`. For example:

```json
{"tool":"get_part_stock","args":{"sku":"CMP-CLUTCH"}}
```

The `get_part_stock` result reflects the adapter's current simplified calculation, not the job-window availability projection in the Feature 2 design. Do not use `reserve_part_for_job` as an authorization boundary; see the safety note below. Stop the adapter with `Ctrl+C`/`Ctrl+Z` followed by Enter in its terminal, as appropriate for the shell.

The adapter makes backend HTTP calls with a fixed five-second timeout. This is not configurable and is not a reservation expiry or hold duration. Its in-memory idempotency cache lasts only for the current adapter process and is not a backend idempotency guarantee.

## Current Limitations and Safety

The requirements, ADR, and MCP tool documents describe a target design, not all implemented behavior. The checked-in backend currently:

- Changes a part's `totalQuantity` directly when the reservation endpoint succeeds; it does not create a job-linked issue record or derive `inUse`/`available` from a job window.
- Does not validate the supplied job id, technician ownership, or job status on the reservation endpoint.
- Has no REST return/release endpoint, although a service method exists.
- Does not implement the documented MCP session-token, dispatch-brief, or human-confirmation checks. The adapter forwards a reservation request without a token.

Consequently, these components are for local development and demonstration only. Do not expose them to untrusted callers, connect them to production inventory, or treat the adapter's confirmation workflow as authorization. The risk assessment records the current write path as **NO-GO for production notes-enabled reservation or auto-approval**. Do not infer that the proposed security design or tool contract is enforced by runtime code.

## Troubleshooting

| Symptom | Check / resolution |
|---|---|
| `mvn` is not recognized or compilation reports an unsupported Java version | Install/configure Java 17 and Maven, then check `java -version` and `mvn -version`. Run Maven from `backend/`. |
| Backend does not start because port 8080 is occupied | Stop the process using the port or set `SERVER_PORT` to another free port. Update `SERVICEFORGE_BACKEND_URL` for the adapter and the frontend's API environment configuration if applicable. |
| Adapter reports it cannot contact the backend | Confirm the backend is running and reachable, then check `SERVICEFORGE_BACKEND_URL` (default `http://localhost:8080`). Adapter HTTP requests time out after five seconds. |
| `GET /api/parts` returns no response or a connection error | Confirm Spring Boot completed startup and use the configured port. The catalog is initialized by the backend at startup. |
| Adapter process exits or prints an error for a request | Send one valid JSON object per line; tool calls require an `args` JSON object. The process returns structured errors for malformed requests and unexpected exceptions. |
| Reservation request returns `400` | Supply a JSON body with a positive integer `quantity`, for example `{ "jobId": 1, "quantity": 1 }`. |
| Reservation request returns `404` | Check the SKU spelling against the seeded list. SKU lookup is case-insensitive in the backend. |
| Reservation request returns `409` | The requested quantity exceeds the part's current stored total. Restarting the local backend resets the in-memory seed; no persisted inventory is used. |
| Adapter reads zero `inUseNow` or reports all stock available | This is a current implementation limitation: the adapter has not implemented the time-window projection. It is not evidence that no job-linked reservation exists. |

For a backend regression check, run `mvn test` from `backend/`. For the Angular build, run `npm run build` from `frontend/`.

## Related References

- [Feature 2 requirements](requirements.md)
- [Feature 2 architecture ADR](architecture-adr.md)
- [MCP tool design](tool-design.md)
- [MCP parts-stock tool contract](tools/parts-stock-tool.md)
- [Feature 2 risk assessment](assessments/risk-assessment-report.md)