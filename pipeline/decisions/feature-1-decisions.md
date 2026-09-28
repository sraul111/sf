# Feature 1 — Decision Log

Decisions made while building Feature 1 that later features may need to retrieve. This file exists so a later feature's memory-retrieval step has something concrete to pull a single fact from, instead of needing the entire build history in context.

## Decision: standard travel buffer per job is 45 minutes

**What was decided:** every booked job reserves not just its own start/end time on a technician's calendar, but an additional 45-minute travel buffer, to account for the technician getting to the next job. This is encoded as `TRAVEL_BUFFER_MINUTES = 45` in `TechnicianAvailabilityService`.

**Why it matters beyond Feature 1:** this buffer is what actually limits how many jobs — and, later, how many parts-reservations tied to those jobs — can realistically be scheduled for one technician in a day. Anything built on top of the calendar that reasons about "how many jobs can fit today" needs this number, not a guess.

**Where it lives in code:** `backend/src/main/java/com/serviceforge/service/TechnicianAvailabilityService.java`, `TRAVEL_BUFFER_MINUTES` constant.

## Decision amendment: bookings use buffered interval conflict detection

**What changed:** `bookJob(...)` now rejects a new job for the same technician when its time range overlaps any existing job's half-open `[startTime, endTime)` range, or when the gap before or after that job is less than the standard 45-minute travel buffer. The earlier exact-start-time-only behavior was a bug and has been fixed.

**Why it matters:** scheduling and later features can rely on bookings for one technician not overlapping and on a minimum 45-minute gap between jobs. A booking exactly 45 minutes away is allowed.

**Where it is defined and verified:** see `pipeline/feature2/rules/no-overlap-booking.md` for the constraint and `backend/src/test/java/com/serviceforge/controller/TechnicianAvailabilityControllerTest.java` for overlap, buffer-conflict, and accepted-booking cases.
