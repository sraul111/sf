# Memory Retrieval Skill

Use this skill when an agent needs to read or retain validated cross-feature memory for Feature 2.

## Location

Run commands from:

```text
pipeline/feature2/memory_pipeline
```

The scripts use the sibling `memory_store.json` file. Do not edit that file by hand during normal pipeline execution.

## Namespace

Every operation is scoped by `MEMORY_NAMESPACE` in the format:

The default is `ServiceForge:feature2-parts:dev`. Set the variable explicitly when working in another branch or environment. Reads return only exact namespace matches, and writes reject payloads whose namespace does not exactly match the active namespace.

PowerShell:

```powershell
$env:MEMORY_NAMESPACE = "ServiceForge:feature2-parts:dev"
```

## Read Memory

```powershell
node read_memory.js
```

The command prints a JSON array to stdout. Parse the result before using it as context. It validates stored records, filters to the active namespace, and applies the current 500-token approximation by dropping the oldest matching records first.

Useful commands:

```powershell
node read_memory.js --help
node read_memory.js | ConvertFrom-Json
```

An invalid namespace or unreadable/invalid store produces a non-zero exit code and an error on stderr. Do not inject partial output after a failed read.

## Write Memory

The command accepts three arguments:

```text
node write_memory.js "<payload-json>" "<source_task>" "<type_tag>"
```

Example:

```powershell
$payload = @{ 
  namespace = $env:MEMORY_NAMESPACE
  validated = $true
  content = "The standard travel buffer per job is 45 minutes."
} | ConvertTo-Json -Compress

node write_memory.js $payload "pipeline/decisions/feature-1-decisions.md" "[FACT]"
```

Allowed type tags are:

```text
[DECISION]
[FACT]
[STATE]
```

The payload must be a JSON object containing:

- `namespace`: a valid `repo:branch:env` value equal to `MEMORY_NAMESPACE`.
- `validated`: exactly `true`.
- `content`: non-empty retained content.

`source_task` must be non-empty and must not contain a newline. The writer rejects content containing known transient, reasoning, unverified, or out-of-scope markers. It appends an ISO-8601 timestamp and writes the record to the JSON store.

## Agent Procedure

1. Set or confirm `MEMORY_NAMESPACE` for the current repository, branch, and environment.
2. Run `node read_memory.js` before making a decision that may depend on prior feature work.
3. Use only returned, validated records as context. Do not treat an empty array as a failed read.
4. After a decision, domain fact, or validated state change is confirmed, create one concise payload and run `write_memory.js`.
5. Check the process exit code and then run `node read_memory.js` to verify that the new record is retrievable in the same namespace.

## Failure Handling

- A non-zero exit code means the operation did not complete; preserve the existing store and report the stderr message.
- Do not retry a rejected payload unchanged. Correct the namespace, schema, type tag, provenance, or content first.
- Do not store raw stdout/stderr, transient failures, intermediate reasoning, conversational filler, or unverified assumptions.
- Do not bypass namespace validation by writing directly to `memory_store.json`.


## When to Use

Use this skill when an agent has a validated architectural decision, domain fact, or validated state change worth retaining across tasks, or when it needs to inspect retained memories for the active namespace. Do not store raw logs, chatter, intermediate reasoning, retry noise, unverified assumptions, or out-of-scope context.

These scripts are manual CLI tools. Writing a record does not create an automatic post-turn hook, and reading a record does not automatically inject it into an agent prompt.

## Prerequisites

- Run commands from `pipeline/feature2/memory_pipeline`.
- Node.js must be available as `node`.
- Set `MEMORY_NAMESPACE` to the exact `repo:branch:env` namespace for the current pipeline context. The default is `ServiceForge:feature2-parts:dev`.
- Optionally set `MEMORY_STORE_PATH` to an alternate JSON store. Normal CLI use defaults to `memory_store.json`; tests and isolated runs should use a temporary store so the committed seed is not modified.
- The namespace must contain exactly three non-empty colon-separated segments with no whitespace.

## Namespace Setup

Windows PowerShell:

```powershell
$env:MEMORY_NAMESPACE = 'ServiceForge:feature2-parts:dev'
```

POSIX shell:

```sh
export MEMORY_NAMESPACE='ServiceForge:feature2-parts:dev'
```

If the variable is unset, both scripts use `ServiceForge:feature2-parts:dev`. A write payload namespace must exactly match the active namespace. A read returns only records with an exact namespace match, preventing branch and environment cross-talk.

When `MEMORY_STORE_PATH` is set, both scripts use that file and derive the concurrency lock from it. The parent directory must already exist.

## Write Memory

Command form on Windows PowerShell:

```powershell
node write_memory.js "<payload-json>" "<source_task>" "<type_tag>"
```

Example:

```powershell
node write_memory.js '{"namespace":"ServiceForge:feature2-parts:dev","validated":true,"content":"The standard travel buffer per job is 45 minutes."}' 'pipeline/decisions/feature-1-decisions.md' '[FACT]'
```

Command form on POSIX shells:

```sh
node write_memory.js "<payload-json>" "<source_task>" "<type_tag>"
```

Example:

```sh
node write_memory.js '{"namespace":"ServiceForge:feature2-parts:dev","validated":true,"content":"Parts reservation reuses the Feature 1 scheduling dependency."}' 'feature-2-parts-reservation' '[DECISION]'
```

The payload must be a JSON object containing:

```json
{
  "namespace": "repo:branch:env",
  "validated": true,
  "content": "A non-empty retained fact"
}
```

Valid type tags are `[DECISION]`, `[FACT]`, and `[STATE]`. `source_task` must be non-empty and must not contain a line break. A successful write appends an ISO-8601 timestamped record and prints `Memory stored.`. Invalid JSON, namespace mismatch, missing validation/content/source, forbidden transient content, or an invalid tag is rejected with a nonzero exit status and does not write a record.

## Read Memory

Windows PowerShell:

```powershell
node read_memory.js
```

POSIX shell:

```sh
node read_memory.js
```

Successful read output is a JSON array on stdout. It contains valid records for the active namespace in store order. Invalid `MEMORY_NAMESPACE`, an invalid store, or a non-array store fails with a nonzero exit status and an error on stderr.

The read path uses the approved approximation of 4 characters per token, calculated over the serialized JSON output. When the selected output exceeds 500 approximate tokens, it removes the oldest matching entry first, then repeats until the output is within the ceiling. This is FIFO retention, not relevance ranking or summarization. When trimming occurs, the JSON array remains on stdout and a `TOKEN_LIMIT_TRIMMED` warning with counts and the approximate token total is written to stderr. The current implementation does not perform hybrid relevance-plus-recency ranking and does not inject the JSON into a prompt automatically.

## Retention Rules

Store only:

- Validated domain facts.
- Key architectural decisions.
- Validated state changes.

Do not store raw stdout/stderr, stack traces, chatter, intermediate reasoning, conversational filler, successfully retried failures, unverified assumptions, or out-of-scope context. Never store secrets such as passwords, API keys, access tokens, private keys, or connection strings. Never store PII such as customer or employee names, email addresses, phone numbers, physical addresses, or identifiers. Never store raw telemetry such as prompts, complete execution traces, workspace or user identifiers, timestamps, model details, or token counts. Redact sensitive values before evaluating whether any remaining content is suitable for retention. Every record carries `timestamp`, `source_task`, `type_tag`, and a payload with `namespace`, `validated: true`, and non-empty `content`.

The CLI applies phrase-based rejection for the named transient categories. Agents remain responsible for semantic judgment before calling the writer. Concurrent local writer processes are serialized by an exclusive lock around the complete read-modify-write transaction; a writer waits up to five seconds and may recover a lock stale for more than thirty seconds. This lock does not coordinate writers on different machines.

## Examples

Read the seeded Feature 1 dependency:

```powershell
$env:MEMORY_NAMESPACE = 'ServiceForge:feature2-parts:dev'
node read_memory.js
```

Write a validated state change in an isolated environment:

```sh
export MEMORY_NAMESPACE='ServiceForge:feature2-parts:test'
node write_memory.js '{"namespace":"ServiceForge:feature2-parts:test","validated":true,"content":"Parts reservation validation is enabled."}' 'feature-2-validation' '[STATE]'
```

## Agent Procedure

1. Classify the candidate as a validated fact, decision, or state change.
2. Remove transient, unverified, and out-of-scope material; set the exact active namespace.
3. Write with the required payload, source task, and valid type tag.
4. Check the exit status and confirmation message.
5. Read when historical context is needed, parse the JSON output, and use only the returned namespace-scoped records.
6. Treat the 500-token/FIFO result as bounded raw retrieval; do not assume automatic prompt injection, semantic ranking, or summarization.
7. During a recognized pipeline, record the read or write outcome in that pipeline's `Memory Operations` run-report section. Include only redacted metadata and never copy memory payloads, secrets, PII, raw prompts, logs, or telemetry into the report.
