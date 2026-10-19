"""
Reference implementation of group expense splitting + debt simplification.

Same situation as the Kotlin project: no javac in this sandbox and no
network to get one, so the algorithm is verified here in Python first,
then mirrored line-for-line into ExpenseSplitter.java.

Two algorithms:
1. calculate_balances: who owes / is owed money overall, from a list of
   expenses (each split equally among participants).
2. simplify_debts: reduces N people's pairwise debts down to the minimum
   number of transactions needed to settle up (greedy max-debtor/max-creditor
   matching) - e.g. instead of A owes B, B owes C, and C owes A all separately,
   collapse it to the smallest necessary set of payments.
"""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class Expense:
    payer: str
    amount: float
    participants: list[str]  # people splitting this expense equally, including the payer if they benefited


@dataclass
class Settlement:
    from_person: str
    to_person: str
    amount: float


def calculate_balances(expenses: list[Expense]) -> dict[str, float]:
    """Positive balance = this person is owed money. Negative = this person owes money."""
    balances: dict[str, float] = {}

    for expense in expenses:
        if not expense.participants:
            continue
        share = expense.amount / len(expense.participants)

        balances[expense.payer] = balances.get(expense.payer, 0.0) + expense.amount
        for person in expense.participants:
            balances[person] = balances.get(person, 0.0) - share

    return {person: round(balance, 2) for person, balance in balances.items()}


def simplify_debts(balances: dict[str, float], epsilon: float = 0.01) -> list[Settlement]:
    """
    Greedy debt simplification: repeatedly match the largest creditor with the
    largest debtor until all balances are settled (within epsilon of zero).
    """
    # Work on a mutable copy; ignore near-zero balances from the start.
    working = {p: b for p, b in balances.items() if abs(b) > epsilon}
    settlements: list[Settlement] = []

    while working:
        creditor = max(working, key=working.get)  # most owed
        debtor = min(working, key=working.get)  # owes the most

        creditor_amount = working[creditor]
        debtor_amount = working[debtor]

        if creditor_amount <= epsilon or debtor_amount >= -epsilon:
            break  # everyone remaining is essentially settled

        settle_amount = round(min(creditor_amount, -debtor_amount), 2)
        settlements.append(Settlement(from_person=debtor, to_person=creditor, amount=settle_amount))

        working[creditor] -= settle_amount
        working[debtor] += settle_amount

        if abs(working[creditor]) <= epsilon:
            del working[creditor]
        if debtor in working and abs(working[debtor]) <= epsilon:
            del working[debtor]

    return settlements
