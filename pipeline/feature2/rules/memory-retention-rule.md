# [RULE] Memory Retention & Token Budget Policy

| Field | Value |
|---|---|
| **Target** | Feature 2 Pipeline Agents |
| **Enforcement** | Mandatory Pre-Storage Filter |
| **Token Ceiling** | 500 Tokens (Hard Limit) |
| **B2 Behavior** | FIFO removal of oldest matching records |

## 1. Discard Policy (Do Not Store)
Agents must filter out and permanently discard the following transient data to prevent context rot:
* Raw stdout/stderr logs and stack traces.
* Chatter, intermediate reasoning steps, or conversational filler.
* Execution failures that were successfully retried.
* Unverified assumptions or out-of-scope domain context.
* Secrets, including passwords, API keys, access tokens, private keys, and connection strings.
* Personally identifiable information (PII), including customer or employee names, email addresses, phone numbers, physical addresses, and identifiers.
* Raw telemetry, including prompts, complete execution traces, workspace or user identifiers, timestamps, model details, and token counts.

Sensitive values must be discarded, not merely tagged as validated. Redact them before evaluating whether any remaining content is suitable for retention.

## 2. Retain Policy (Must Store)
Only the following high-value facts are permitted into the Episodic Memory store:
* **Validated Domain Facts:** Specifically, the Feature 1 scheduling dependency ("the standard travel buffer per job is 45 minutes").
* **Key Architectural Decisions:** Changes to data models or cross-feature dependencies.


## 3. Storage & Provenance Tagging
Every retained memory must be stored with the following metadata tags before being written to the database or JSON store:
* `source_task`: The exact task or agent that generated the memory.
* `timestamp`: ISO-8601 execution time.
* `type_tag`: `[DECISION]`, `[FACT]`, or `[STATE]`.

## 4. Token Budget Enforcement
The B2 CLI read path must never return more than **500 approximate tokens**, calculated as 4 characters per token. If matching records exceed this limit, `read_memory.js` removes the oldest matching records first until the result is within the ceiling. This is deterministic FIFO trimming; it is not relevance ranking or summarization.

The production design may replace FIFO trimming with relevance-and-recency ranking followed by hierarchical consolidation, importance weighting, or another approved summarization strategy. Those capabilities are deferred and are not requirements of the B2 CLI.