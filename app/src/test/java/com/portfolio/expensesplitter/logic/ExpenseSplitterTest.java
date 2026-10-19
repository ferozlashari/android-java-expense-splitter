package com.portfolio.expensesplitter.logic;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

/**
 * Mirrors tests/test_all.py exactly. The Python version has been run and
 * passes 16/16 in this environment (no javac available here - see README).
 * Run this with `./gradlew test` once you have a JDK.
 */
public class ExpenseSplitterTest {

    @Test
    public void payerBalanceReflectsWhatOthersOwe() {
        List<ExpenseSplitter.Expense> expenses = Collections.singletonList(
            new ExpenseSplitter.Expense("Alice", 30.0, Arrays.asList("Alice", "Bob", "Carol"))
        );
        Map<String, Double> balances = ExpenseSplitter.calculateBalances(expenses);
        assertEquals(20.0, balances.get("Alice"), 0.001);
        assertEquals(-10.0, balances.get("Bob"), 0.001);
    }

    @Test
    public void balancesSumToZero() {
        List<ExpenseSplitter.Expense> expenses = Collections.singletonList(
            new ExpenseSplitter.Expense("Alice", 30.0, Arrays.asList("Alice", "Bob", "Carol"))
        );
        Map<String, Double> balances = ExpenseSplitter.calculateBalances(expenses);
        double sum = balances.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0.0, sum, 0.001);
    }

    @Test
    public void multipleExpensesNetCorrectly() {
        List<ExpenseSplitter.Expense> expenses = Arrays.asList(
            new ExpenseSplitter.Expense("Alice", 60.0, Arrays.asList("Alice", "Bob", "Carol")),
            new ExpenseSplitter.Expense("Bob", 30.0, Arrays.asList("Alice", "Bob", "Carol"))
        );
        Map<String, Double> balances = ExpenseSplitter.calculateBalances(expenses);
        assertEquals(30.0, balances.get("Alice"), 0.001);
        assertEquals(0.0, balances.get("Bob"), 0.001);
    }

    @Test
    public void simpleTwoPersonDebtSettlesInOneTransaction() {
        Map<String, Double> balances = new LinkedHashMap<>();
        balances.put("Alice", 20.0);
        balances.put("Bob", -20.0);

        List<ExpenseSplitter.Settlement> settlements = ExpenseSplitter.simplifyDebts(balances);
        assertEquals(1, settlements.size());
        assertEquals("Bob", settlements.get(0).fromPerson);
        assertEquals("Alice", settlements.get(0).toPerson);
        assertEquals(20.0, settlements.get(0).amount, 0.001);
    }

    @Test
    public void threePersonCaseUsesMinimalTransactions() {
        Map<String, Double> balances = new LinkedHashMap<>();
        balances.put("Alice", 10.0);
        balances.put("Bob", 10.0);
        balances.put("Carol", -20.0);

        List<ExpenseSplitter.Settlement> settlements = ExpenseSplitter.simplifyDebts(balances);
        assertEquals(2, settlements.size());

        double total = settlements.stream().mapToDouble(s -> s.amount).sum();
        assertEquals(20.0, total, 0.01);
    }

    @Test
    public void alreadySettledGroupProducesNoTransactions() {
        Map<String, Double> balances = new LinkedHashMap<>();
        balances.put("Alice", 0.0);
        balances.put("Bob", 0.0);

        List<ExpenseSplitter.Settlement> settlements = ExpenseSplitter.simplifyDebts(balances);
        assertEquals(0, settlements.size());
    }

    @Test
    public void applyingSettlementsZerosOutBalances() {
        Map<String, Double> balances = new LinkedHashMap<>();
        balances.put("Alice", 45.50);
        balances.put("Bob", -20.25);
        balances.put("Carol", -25.25);

        List<ExpenseSplitter.Settlement> settlements = ExpenseSplitter.simplifyDebts(balances);

        Map<String, Double> resulting = new LinkedHashMap<>(balances);
        for (ExpenseSplitter.Settlement s : settlements) {
            resulting.merge(s.fromPerson, s.amount, Double::sum);
            resulting.merge(s.toPerson, -s.amount, Double::sum);
        }
        for (double balance : resulting.values()) {
            assertTrue("balance should be ~zero after settling, was " + balance, Math.abs(balance) < 0.02);
        }
    }

    @Test
    public void endToEndGroupTripScenario() {
        List<ExpenseSplitter.Expense> expenses = Arrays.asList(
            new ExpenseSplitter.Expense("Alice", 120.0, Arrays.asList("Alice", "Bob", "Carol", "Dave")),
            new ExpenseSplitter.Expense("Bob", 80.0, Arrays.asList("Alice", "Bob", "Carol", "Dave")),
            new ExpenseSplitter.Expense("Carol", 40.0, Arrays.asList("Alice", "Bob", "Carol", "Dave"))
        );
        Map<String, Double> balances = ExpenseSplitter.calculateBalances(expenses);
        List<ExpenseSplitter.Settlement> settlements = ExpenseSplitter.simplifyDebts(balances);

        assertTrue("should use at most 3 transactions for 4 people", settlements.size() <= 3);

        Map<String, Double> resulting = new LinkedHashMap<>(balances);
        for (ExpenseSplitter.Settlement s : settlements) {
            resulting.merge(s.fromPerson, s.amount, Double::sum);
            resulting.merge(s.toPerson, -s.amount, Double::sum);
        }
        for (double balance : resulting.values()) {
            assertTrue(Math.abs(balance) < 0.02);
        }
    }
}
