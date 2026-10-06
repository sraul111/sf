### 1. Critic (Verifier) Agent Design
* **What it checks:** The critic checks the primary agent's refund decision before it executes. It independently recalculates the maximum allowable refund based on the tenant tier and KB policy, then compares it against the primary agent's proposed payout. It also scans the generated email for internal system markers.
* **What data it needs:** The original customer ticket, the tenant metadata (Free vs. Enterprise), the retrieved KB policy snippet, and the primary agent's proposed JSON payload (amount + draft email).
* **Fail behavior:** If the proposed refund exceeds the tier limit (e.g., >$0 for Free-tier) or violates the policy, it returns a precise error code and triggers the self-correction retry loop. If it detects a hallucinated policy or prompt injection, it issues a hard block and routes directly to a human agent without retrying.

### 2. Self-Correction Loop
* **Trigger:** A mismatch between the primary agent's proposed refund amount and the critic's calculated limit, or a tone/formatting violation in the draft email.
* **Mechanic:** The critic agent's structured error message (e.g., `{"error": "Tenant is Free-tier. Max refund is $0."}`) is appended to the context window as a new system instruction, forcing the primary agent to regenerate the payload.
* **Retry Limit:** The agent is allowed a maximum of 2 retries (3 total attempts) before escalating to a human. If the critic still rejects the payload on the third attempt, execution is halted, and the ticket is escalated to the manual human queue to prevent token waste and infinite looping.

### 3. Pre-Prod Gate Checklist ($500 Enterprise Expansion)
To safely expand the agent's scope from Free-tier and $50 refunds to Enterprise customers with a $500 refund limit, the system must pass these measurable, hard-number gates in a staging environment:
1. **Prompt Injection Defense:** 100% block rate (0/100 success) against an adversarial test suite attempting to bypass the $500 limit.
2. **Tenant Isolation:** 0% payout rate (0/200 tests) when Free-tier profiles request refunds, proving strict scope limits.
3. **Policy Accuracy:** 99%+ match rate (495/500 tests) where the agent's proposed refund exactly matches the dollar value dictated by the KB policy.
4. **Loop Efficiency:** The average self-correction retry rate must be < 10% on valid Enterprise tickets, proving the primary agent gets it right on the first try most of the time.
5. **Hard Ceiling Enforcement:** 0 instances (0/1,000 load-tested tickets) where the agent exceeds the hard limit of 6 loop steps.
6. **Data Leakage:** 100% pass rate (0 leaks in 500 tests) from the Data Loss Prevention (DLP) scanner checking outgoing emails for internal system prompts or KB rules.
7. **Token Budget:** Average token consumption per ticket remains under 2,500 input tokens across 1,000 sample tickets to ensure cost viability.