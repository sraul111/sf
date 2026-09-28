# Memory Pipeline Audit

**Audited implementation:** `pipeline/feature2/memory_pipeline/`

**Reference design:** `pipeline/feature2/memory-design.md`

**Audit date:** 2026-09-25

## Executive Summary

The implementation is a working validation slice of the proposed memory architecture. It provides a persistent JSON store, exact namespace isolation, allow-listed memory types, provenance, retention filtering, and a deterministic 500-token approximation. The test suite passes all 8 tests.

It does not yet implement the complete design. In particular, writes are synchronous CLI operations rather than a non-blocking post-turn hook, reads are not ranked by recency and relevance, and there is no storage abstraction or JIT/context-window integration. These are design gaps, not failures of the current test suite.

## Verification

Command executed from `pipeline/feature2/memory_pipeline/`:

```text
node --test memory_pipeline.test.js
```

Result: **9 passed, 0 failed**.

The tests restore `memory_store.json` after execution. The committed store remains seeded with the Feature 1 travel-buffer fact.

## Findings Against the Design

### 1. Memory taxonomy and context mapping

| Design requirement | Status | Evidence / assessment |
|---|---|---|
| Retain decisions, facts, and validated state | Implemented | `write_memory.js` accepts only `[DECISION]`, `[FACT]`, and `[STATE]`, and requires `validated: true`. |
| Preserve the Feature 1 45-minute dependency | Implemented | `memory_store.json` contains the validated `[FACT]` record sourced from `pipeline/decisions/feature-1-decisions.md`. |
| Store provenance metadata | Implemented | Each record gets an ISO timestamp, `source_task`, `type_tag`, and payload. |
| Inject retrieved memory into working context | Not implemented | `read_memory.js` writes JSON to stdout, but no agent hook or prompt-injection integration consumes it. |
| Store semantic/procedural memory in the documented instruction and skill files | Partially implemented | Repository instructions exist, and this audit adds the retrieval skill. The scripts themselves do not manage those static/procedural tiers. |

### 2. Namespace and scoping

| Design requirement | Status | Evidence / assessment |
|---|---|---|
| Use `repo:branch:env` namespaces | Implemented | Both scripts validate the three-segment, non-whitespace format. |
| Enforce the active namespace on writes | Implemented | A payload namespace must exactly match `MEMORY_NAMESPACE` or the default `ServiceForge:feature2-parts:dev`. |
| Return only the active namespace on reads | Implemented | Reads filter records by exact namespace; tests cover branch and environment neighbors. |

The namespace parser is intentionally structural. It does not maintain an allow-list of known repositories, branches, or environments; callers are responsible for selecting the intended namespace.

### 3. Storage and JIT retrieval

| Design requirement | Status | Evidence / assessment |
|---|---|---|
| Begin with persistent JSON storage | Implemented | `memory_store.json` is read and updated by the scripts. Writes use an exclusive lock, a process-specific temporary file, and rename. |
| Keep an abstraction path toward PostgreSQL/pgvector | Not implemented | Storage access is directly coupled to the JSON file in both scripts; there is no storage interface or adapter boundary. |
| Retrieve by relevance to the current reservation task | Not implemented | `read_memory.js` has no query or task argument and returns every valid record in the namespace. |
| Rank using recency and relevance | Not implemented | Results preserve store order. There is no score, recency sort, or relevance ranking. |
| Enforce a JIT injection ceiling | Partially implemented | The reader applies a 500-token approximation to serialized JSON, drops oldest records FIFO, and emits `TOKEN_LIMIT_TRIMMED` metadata on stderr when records are removed. It is a transport-size limit, not a context injection or summarization pipeline. |
| Compress over-budget relevant facts | Not implemented | Records are discarded from the front until under budget; no hierarchical consolidation, importance weighting, or summarization occurs. |

The current ceiling can also discard all records when one serialized record exceeds the budget. That is consistent with the implemented FIFO policy, but it is not equivalent to the design's requested compression behavior.

### 4. Retain/discard policy

| Design requirement | Status | Evidence / assessment |
|---|---|---|
| Discard raw logs and transient reasoning | Partially implemented | A case-insensitive content filter rejects terms such as `stdout`, `stderr`, `stack trace`, and `intermediate reasoning`. This is a keyword heuristic, not semantic classification. |
| Discard unverified or out-of-scope content | Partially implemented | The writer requires `validated: true` and rejects matching phrases such as `unverified` and `out-of-scope`; it does not independently verify the claim or determine domain scope. |
| Retain validated decisions, facts, and state | Implemented | Validated payloads with the three supported type tags are appended with provenance. |
| Write only approved post-turn artifacts | Not implemented | Any caller able to invoke the CLI can submit a syntactically valid record; there is no post-turn event hook or approval stage. |

## Strengths

- The CLI has explicit help output and non-zero exits for malformed input.
- Writes validate before changing the store, and the temporary-file rename avoids leaving a partially written final file during a normal write.
- Read results are deterministic, and over-budget reads report `TOKEN_LIMIT_TRIMMED` metadata, which makes the current pipeline easy to test and reproduce.
- The tests cover schema, persistence across processes, retention rejection, namespace isolation, invalid reads, FIFO trimming, and the token approximation.

## Risks and Follow-up

1. **Concurrent writes:** local concurrent invocations are serialized by the lock file and covered by a concurrent-writer test. The lock coordinates processes sharing the same filesystem; a multi-machine deployment still needs a transactional storage adapter or a single-writer service before distributed post-turn hooks are enabled.
2. **Retrieval quality:** returning all namespace records does not provide task-specific JIT retrieval and will become increasingly lossy as FIFO trimming removes older facts.
3. **Validation trust boundary:** `validated: true` is caller-supplied. A future hook should assign validation status after filtering and provenance checks rather than accepting it as an assertion.
4. **Token accounting:** four characters per token is a documented approximation and counts JSON metadata as well as content. A production adapter should use the target model tokenizer or a conservative budget service.
5. **Atomicity cleanup:** if rename fails, the temporary file may remain. The writer should clean it up or report recovery guidance when the storage layer is hardened.

## Audit Conclusion

The implementation satisfies the namespace, schema, provenance, seeded-fact, basic retention, persistence, and bounded-output portions of the design validation slice. It should be treated as a local JSON-backed CLI prototype, not as the complete episodic-memory and JIT-retrieval architecture described in `memory-design.md`.
# Memory Pipeline Audit Report

**Audit date:** 2026-09-25  
**Scope:** The running implementation under `pipeline/feature2/memory_pipeline/`, compared with `pipeline/feature2/memory-design.md` and `pipeline/feature2/rules/memory-retention-rule.md`. This audit covers the scripts, current store contents, and the existing CLI test file. It does not change or assess unrelated application code.

## Executive Summary

The implemented slice is a validated, namespace-scoped JSON CLI store. It supports the approved JSON-array record schema, provenance fields, the `MEMORY_NAMESPACE` boundary, the `[DECISION]`/`[FACT]`/`[STATE]` type tags, retention rejection for the rule's named transient categories, a four-characters-per-token approximation, and deterministic FIFO removal of oldest matching entries over the 500-token ceiling.

The implementation is not the complete architecture described in the design. It is a manual write/read tool, not an automatic post-turn pipeline. It does not perform hybrid relevance-plus-recency ranking, summarization, semantic/procedural/working-tier integration, or provide a pgvector abstraction. The current store contains one validated Feature 1 travel-buffer fact in the default namespace.

## Evidence and Current State


## Requirement Traceability

| Requirement | Status | Evidence / assessment |
|---|---|---|
| Store records as a JSON array with timestamp, source task, type tag, and payload | MET | `memory_store.json`; write and read schema checks in `write_memory.js` and `read_memory.js`. |
| Payload has a `repo:branch:env` namespace and validated content | MET | `write_memory.js` requires a valid namespace, exact active namespace, `validated: true`, and non-empty content. |
| `MEMORY_NAMESPACE` scopes reads and writes | MET | Both scripts use `MEMORY_NAMESPACE`, defaulting to `ServiceForge:feature2-parts:dev`; records are matched exactly. |
| Retain only validated facts, decisions, and state changes with provenance | PARTIAL | Validation and the three tags are enforced. The content filter is rule-pattern based, so it cannot semantically identify every possible chatter, assumption, or transient failure. |
| Discard named transient content | MET | `FORBIDDEN_CONTENT` rejects stdout/stderr, stack traces, intermediate reasoning, conversational filler, successful retries, unverified content, and out-of-scope content. |
| Use valid type tags `[DECISION]`, `[FACT]`, and `[STATE]` | MET | `TYPE_TAGS` is enforced by both write validation and read validation. |
| Enforce a hard 500-token read ceiling | PARTIAL | `read_memory.js` uses `Math.ceil(serializedLength / 4)` and removes oldest matches. It does not summarize or consolidate records when the limit is exceeded. |
| Use 4 characters per token | MET | `tokenCount()` in `read_memory.js` implements the approved approximation. |
| Remove the oldest entries FIFO when over budget | MET | Namespace-filtered records preserve store order; `selected.shift()` removes the oldest first. |
| Seed the Feature 1 travel-buffer dependency | MET | The current store contains the validated 45-minute fact from `pipeline/decisions/feature-1-decisions.md`. |
| Inject just-in-time memories into the active working context | NOT IMPLEMENTED | The scripts print or store JSON only; no caller, prompt integration, or automatic injection exists. |
| Rank results by hybrid relevance and recency | NOT IMPLEMENTED | Reads retain store order and apply only FIFO trimming; there is no relevance score or recency ranking. |
| Run a non-blocking post-turn event hook | NOT IMPLEMENTED | No hook or event integration exists in the inspected pipeline directory. |
| Integrate semantic, procedural, episodic, and working memory tiers | NOT IMPLEMENTED | The JSON store and CLI exist, but no tier orchestration or working-context integration is implemented. |
| Provide a storage abstraction ready for Postgres/pgvector hybrid search | NOT IMPLEMENTED | Storage is directly read and written as `memory_store.json`; no abstraction or pgvector adapter exists. |

The JSON array schema, `MEMORY_NAMESPACE` behavior, 4 characters/token approximation, and FIFO oldest-entry removal are treated here as approved implementation contracts. Hybrid relevance-plus-recency ranking, the post-turn hook, semantic/procedural/working tier integration, and the pgvector abstraction remain design aspirations, not current contracts.

## Risks and Gaps


## Recommendations

1. Keep the current CLI contracts and document them for pipeline agents before introducing broader integration.
2. Add a retrieval layer that scores relevance and recency, then summarizes selected content before applying the 500-token injection ceiling.
3. Add the post-turn filter/hook only after defining its caller, failure handling, and provenance contract.
4. Introduce a storage interface before adding pgvector, with the JSON store retained as the validation backend.
5. Replace or supplement regex retention checks with explicit agent-side classification and focused negative/positive cases for each retention rule category.
