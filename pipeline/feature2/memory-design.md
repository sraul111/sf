# [DESIGN] Memory Architecture Specification

| Field | Value |
|---|---|
| **Feature** | Feature 2 — Parts Reservation |
| **Module** | Module B1 |
| **Status** | Proposed production design; B2 CLI prototype implemented |

## 1. Memory Taxonomy & Context Mapping
To prevent context window bloat and ensure our agents can reliably recall necessary cross-feature dependencies, Feature 2 ("Parts Reservation") will distribute its context across the following four memory tiers[cite: 1]:

*   **Semantic Memory (Static Context):** 
    *   *Storage:* `.github/copilot-instructions.md` and repository rule files[cite: 1].
    *   *Content:* Project-wide constraints (e.g., Java 17, Spring Boot 3, Angular), coding standards, and static domain rules.
*   **Procedural Memory (Agent Capabilities):** 
    *   *Storage:* Agent Skills and MCP Tool definitions (`[SKILL]` and `[TOOL]` files)[cite: 1].
    *   *Content:* The execution steps for the parts-stock checking tool and the memory-retrieval skill itself.
*   **Episodic Memory (Long-Term/Historical Fact Store):** 
    *   *Storage:* Vector DB (e.g., pgvector) or persistent JSON store[cite: 1].
    *   *Content:* Cross-feature operational decisions and past bug fixes. Critically, this must store the Feature 1 scheduling dependency: "the standard travel buffer per job is 45 minutes"[cite: 4]. 
*   **Working Memory (Short-Term/Context Window):**
    *   *Storage:* The active LLM prompt context window[cite: 1].
    *   *Content:* The immediate user prompt, the specific job ID being booked, and the Just-In-Time (JIT) memory injected from the Episodic store during the current turn.

## 2. Namespace & Scoping Boundaries
To prevent data leakage between different environments or concurrent feature branches, all episodic memories will be strictly scoped using a hierarchical namespace pattern[cite: 1]:
*   **Format:** `repo:branch:env` (e.g., `ServiceForge:feature2-parts:dev`)[cite: 1].
*   **Enforcement:** The memory retrieval skill must validate the namespace before returning any historical facts to the active context.

## 3. Storage Backend & JIT Retrieval Strategy
*   **Production write path:** A non-blocking post-turn event hook will filter execution outputs. Decisions, domain facts, and dependencies will be stored with full provenance metadata (source_task, timestamp, type_tag, status)[cite: 1].
*   **B2 write path:** The current implementation is a manual `write_memory.js` CLI backed by `memory_store.json`. It validates and persists one candidate at a time and uses an exclusive lock to serialize concurrent processes on the same filesystem; no automatic post-turn hook is implemented yet.
*   **Production storage backend:** Initially implemented as an `InMemoryStore` (or `memory_store.json`) for pipeline validation, with an abstraction layer ready for `Postgres+pgvector` hybrid search[cite: 1].
*   **Production read path (JIT injection):** Memories will be queried during the execution pipeline and ranked using a hybrid scoring mechanism combining both *recency* and *relevance* to the current reservation task[cite: 1].
*   **B2 read path:** The current `read_memory.js` CLI filters by the exact `MEMORY_NAMESPACE` and returns records in store order. It applies a deterministic 500-token approximation by dropping the oldest matching records first (FIFO).

## 4. Retain/Discard Policy & Token Budgeting
Injecting raw history leads to context rot. We will enforce strict filters and ceilings on what enters Working Memory[cite: 1]:

*   **Discard:** Raw stdout logs, chatter, transient execution failures, and intermediate agent reasoning steps must be discarded[cite: 1].
*   **Retain:** Key architectural decisions, verified domain facts (like the 45-minute overlap rule), and validated state changes[cite: 1].
*   **Production token ceiling:** The JIT retrieval pipeline will enforce a hard limit of **500 tokens** per turn for injected memory[cite: 1]. If the relevant pulled facts exceed this budget, a summarization technique (hierarchical consolidation or importance weighting) will compress the payload before injection[cite: 1].
*   **B2 token behavior:** The CLI uses a 4-characters-per-token approximation and removes the oldest matching records until the serialized result is within 500 tokens. It does not rank, summarize, or consolidate records.

## 5. B2 Implementation Contract

B2 is a local JSON-backed CLI prototype. Its approved contracts are exact namespace isolation, validated record persistence, a 500-token approximation, and FIFO removal of the oldest matching records when the ceiling is exceeded. Reads do not accept a retrieval query or context argument and are not automatically injected into agent prompts.

Concurrent local writer processes are serialized by a lock file around the complete read-modify-write transaction. A writer waits up to five seconds for the lock and can recover a lock left stale for more than thirty seconds. This protects a shared local filesystem only; multi-machine deployment still requires a transactional storage service or a single writer queue.

## 6. Deferred Production Capabilities

The following remain intended production capabilities rather than B2 behavior:

* Hybrid relevance-and-recency ranking.
* Hierarchical consolidation, importance weighting, or other summarization when the token budget is exceeded.
* Automatic post-turn writes and JIT prompt injection.
* A storage abstraction backed by PostgreSQL and pgvector.