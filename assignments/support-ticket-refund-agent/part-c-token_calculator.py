def calculate_costs(system_chars=900, kb_chars=5000, ticket_chars=300, steps=6, tickets_per_day=8000):
    # 1. Base tokens per step (4 chars = 1 token heuristic)
    base_input_tokens = (system_chars + kb_chars + ticket_chars) / 4
    output_tokens_per_step = 150
    
    # 2. Naive Input Calculation
    naive_input_total = sum(base_input_tokens + (step * output_tokens_per_step) for step in range(steps))
    
    # 3. Optimized Input Calculation (Summary holds at 150 tokens)
    optimized_input_total = base_input_tokens + ((steps - 1) * (base_input_tokens + 150))
    
    # 4. Daily Costs
    total_output_tokens_day = tickets_per_day * steps * output_tokens_per_step
    output_cost_day = (total_output_tokens_day / 1_000_000) * 15
    
    naive_cost_day = ((tickets_per_day * naive_input_total) / 1_000_000) * 3 + output_cost_day
    optimized_cost_day = ((tickets_per_day * optimized_input_total) / 1_000_000) * 3 + output_cost_day
    
    print(f"Base Input Tokens/Step: {base_input_tokens}")
    print(f"Naive Tokens/Ticket: {naive_input_total}")
    print(f"Optimized Tokens/Ticket: {optimized_input_total}")
    print(f"Daily Cost (Naive): ${naive_cost_day:.2f}")
    print(f"Daily Cost (Optimized): ${optimized_cost_day:.2f}")

if __name__ == "__main__":
    calculate_costs()