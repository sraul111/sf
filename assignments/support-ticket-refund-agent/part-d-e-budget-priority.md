# Part D: Token Waste Audit & Budget Enforcement

### 1. Token Waste Diagnosis
The naive design suffers massively from **Context History Bloat (Unnecessary Context)**. By blindly appending the full raw history to every step, the input inflates from 1,550 tokens in Step 1 to 2,300 tokens by Step 6. 
* **The Cost:** Our math proves this single flaw wastes exactly 1,500 tokens per ticket. Across 8,000 daily tickets, this specific waste costs an unnecessary $36 a day (or over $13,000 a year) just to have the LLM repeatedly re-read its own earlier drafts.

### 2. Context-Zone Budget & KB Tension
To aggressively control costs, we enforce a strict per-step context-zone budget[cite: 5]. The main tension is the 5,000-character KB document (~1,250 tokens), which is too heavy to load in full every time[cite: 5]. 

* **System Prompt & Few-Shot:** Cap at 300 tokens (strict, concise operational instructions only)[cite: 5].
* **User Input (Ticket):** Cap at 150 tokens (easily fits the ~300 character average, truncates abusive essay-length tickets)[cite: 5].
* **Retrieved Docs (KB):** Cap at 400 tokens[cite: 5]. *Tension Resolution:* We will not inject the entire 5,000-character document. Instead, we use Top-K vector chunking to inject only the 2-3 most relevant paragraphs matching the ticket's specific issue[cite: 5].
* **Response Generation:** Cap at 200 tokens (handles the 150-token average draft with a small buffer)[cite: 5].
* **Total Budget per Step:** ~1,050 tokens (a 32% reduction from the original 1,550 base).

### 3. Circuit-Breaker Behavior
Tying directly back to Guardrail #1: If a ticket hits the 6-loop-step limit or blows past a hard ceiling of 2,500 total consumed input tokens during its lifecycle, the circuit breaker fires instantly[cite: 5]. It forcefully halts the execution, blocks any API payout tools, tags the ticket with `ERR_TOKEN_CEILING_EXCEEDED`, and routes it directly to the human manual queue. No further LLM calls are permitted for that ticket.

---

# Part E: Deliverables & Sprint Prioritization

### What I would cut first if given only 1 sprint
If I had only one sprint to deliver this feature, **I would completely cut the Critic Agent's self-correction loop and the autonomous $50 payout execution**[cite: 5]. 

Instead of building a complex multi-agent retry system that automatically sends money, I would ship a "Draft-Only" agent. The primary agent would still read the ticket, search the KB, and draft the refund response, but it would pause for a human agent to click a single "Approve & Send" button. 

**Why cut this first?** 
Building autonomous financial execution and the safety loops required to protect it takes massive engineering effort and staging validation (the Pre-Prod Gate). By shipping the draft-only version in Sprint 1, we instantly speed up the human support team (saving them reading and typing time) while taking on zero financial risk. We can monitor the agent's baseline accuracy in production, gather real token usage data, and build the automated execution and critic loops in Sprint 2 once the foundation is proven.