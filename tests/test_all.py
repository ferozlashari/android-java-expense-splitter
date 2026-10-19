"""Test suite for expense_splitter.py. Run with: python3 tests/test_all.py"""

import os
import sys

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "reference-impl")))

from expense_splitter import Expense, calculate_balances, simplify_debts

passed = 0
failed = 0


def check(name, condition):
    global passed, failed
    if condition:
        passed += 1
        print(f"  PASS: {name}")
    else:
        failed += 1
        print(f"  FAIL: {name}")


print("== calculate_balances ==")

# Alice pays $30 for a dinner split 3 ways (Alice, Bob, Carol)
expenses = [Expense(payer="Alice", amount=30.0, participants=["Alice", "Bob", "Carol"])]
balances = calculate_balances(expenses)
check("payer's balance reflects what others owe them", balances["Alice"] == 20.0)  # paid 30, owes 10, net +20
check("participant balance is negative (owes money)", balances["Bob"] == -10.0)
check("balances sum to zero (money isn't created or destroyed)", abs(sum(balances.values())) < 0.001)

# Multiple expenses, different payers
expenses = [
    Expense(payer="Alice", amount=60.0, participants=["Alice", "Bob", "Carol"]),  # each owes 20
    Expense(payer="Bob", amount=30.0, participants=["Alice", "Bob", "Carol"]),     # each owes 10
]
balances = calculate_balances(expenses)
check("multiple expenses net out correctly for Alice", balances["Alice"] == 30.0)  # +60-20 -10 = 30
check("multiple expenses net out correctly for Bob (paid 30, owes 20+10)", balances["Bob"] == 0.0)
check("balances still sum to zero across multiple expenses", abs(sum(balances.values())) < 0.001)

print("== simplify_debts ==")

# Simple 2-person case
balances = {"Alice": 20.0, "Bob": -20.0}
settlements = simplify_debts(balances)
check("simple 2-person debt: exactly one settlement", len(settlements) == 1)
check("settlement direction is correct (debtor pays creditor)", settlements[0].from_person == "Bob" and settlements[0].to_person == "Alice")
check("settlement amount matches the debt", settlements[0].amount == 20.0)

# 3-person cycle should still simplify to minimal transactions
balances = {"Alice": 10.0, "Bob": 10.0, "Carol": -20.0}
settlements = simplify_debts(balances)
total_settled = sum(s.amount for s in settlements)
check("3-person case settles the full amount owed", abs(total_settled - 20.0) < 0.01)
check("3-person case uses minimal transactions (2, not 3+)", len(settlements) == 2)

# Already-settled group (everyone at zero)
balances = {"Alice": 0.0, "Bob": 0.0}
settlements = simplify_debts(balances)
check("already-settled group produces no transactions", len(settlements) == 0)

# Verify settlements actually zero out the balances when applied
balances = {"Alice": 45.50, "Bob": -20.25, "Carol": -25.25}
settlements = simplify_debts(balances)
resulting_balances = dict(balances)
for s in settlements:
    resulting_balances[s.from_person] += s.amount
    resulting_balances[s.to_person] -= s.amount
check(
    "applying all settlements brings every balance to ~zero",
    all(abs(b) < 0.02 for b in resulting_balances.values()),
)

print("== end-to-end: realistic group trip scenario ==")

expenses = [
    Expense(payer="Alice", amount=120.0, participants=["Alice", "Bob", "Carol", "Dave"]),  # dinner
    Expense(payer="Bob", amount=80.0, participants=["Alice", "Bob", "Carol", "Dave"]),       # hotel
    Expense(payer="Carol", amount=40.0, participants=["Alice", "Bob", "Carol", "Dave"]),      # gas
]
balances = calculate_balances(expenses)
settlements = simplify_debts(balances)

check("group scenario balances sum to zero", abs(sum(balances.values())) < 0.01)
resulting = dict(balances)
for s in settlements:
    resulting[s.from_person] += s.amount
    resulting[s.to_person] -= s.amount
check("group scenario settlements fully resolve all balances", all(abs(b) < 0.02 for b in resulting.values()))
check("group scenario uses a reasonable number of transactions (<=3 for 4 people)", len(settlements) <= 3)

print(f"\n{passed} passed, {failed} failed")
sys.exit(1 if failed else 0)
