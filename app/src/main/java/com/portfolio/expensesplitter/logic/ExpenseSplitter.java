package com.portfolio.expensesplitter.logic;

import java.util.*;

/**
 * Expense balance + debt simplification logic. Pure Java, no Android
 * dependencies. Mirrors reference-impl/expense_splitter.py line-for-line -
 * that Python version was run and tested (16/16 passing) in this project;
 * see tests/test_all.py. No javac is available in this sandbox to compile
 * this file directly (see README) - ExpenseSplitterTest.java has the JUnit
 * port of the same test cases, ready for `./gradlew test`.
 */
public class ExpenseSplitter {

    public static class Expense {
        public final String payer;
        public final double amount;
        public final List<String> participants;

        public Expense(String payer, double amount, List<String> participants) {
            this.payer = payer;
            this.amount = amount;
            this.participants = participants;
        }
    }

    public static class Settlement {
        public final String fromPerson;
        public final String toPerson;
        public final double amount;

        public Settlement(String fromPerson, String toPerson, double amount) {
            this.fromPerson = fromPerson;
            this.toPerson = toPerson;
            this.amount = amount;
        }
    }

    /** Positive balance = owed money. Negative = owes money. */
    public static Map<String, Double> calculateBalances(List<Expense> expenses) {
        Map<String, Double> balances = new LinkedHashMap<>();

        for (Expense expense : expenses) {
            if (expense.participants.isEmpty()) continue;
            double share = expense.amount / expense.participants.size();

            balances.merge(expense.payer, expense.amount, Double::sum);
            for (String person : expense.participants) {
                balances.merge(person, -share, Double::sum);
            }
        }

        Map<String, Double> rounded = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            rounded.put(entry.getKey(), round2(entry.getValue()));
        }
        return rounded;
    }

    /** Greedy debt simplification: match largest creditor with largest debtor until settled. */
    public static List<Settlement> simplifyDebts(Map<String, Double> balances, double epsilon) {
        Map<String, Double> working = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : balances.entrySet()) {
            if (Math.abs(entry.getValue()) > epsilon) {
                working.put(entry.getKey(), entry.getValue());
            }
        }

        List<Settlement> settlements = new ArrayList<>();

        while (!working.isEmpty()) {
            String creditor = maxByValue(working);
            String debtor = minByValue(working);

            double creditorAmount = working.get(creditor);
            double debtorAmount = working.get(debtor);

            if (creditorAmount <= epsilon || debtorAmount >= -epsilon) {
                break;
            }

            double settleAmount = round2(Math.min(creditorAmount, -debtorAmount));
            settlements.add(new Settlement(debtor, creditor, settleAmount));

            working.put(creditor, working.get(creditor) - settleAmount);
            working.put(debtor, working.get(debtor) + settleAmount);

            if (Math.abs(working.get(creditor)) <= epsilon) {
                working.remove(creditor);
            }
            if (working.containsKey(debtor) && Math.abs(working.get(debtor)) <= epsilon) {
                working.remove(debtor);
            }
        }

        return settlements;
    }

    public static List<Settlement> simplifyDebts(Map<String, Double> balances) {
        return simplifyDebts(balances, 0.01);
    }

    private static String maxByValue(Map<String, Double> map) {
        return Collections.max(map.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private static String minByValue(Map<String, Double> map) {
        return Collections.min(map.entrySet(), Map.Entry.comparingByValue()).getKey();
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
