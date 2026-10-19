package com.portfolio.expensesplitter.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.portfolio.expensesplitter.R;
import com.portfolio.expensesplitter.data.ExpenseDbHelper;
import com.portfolio.expensesplitter.logic.ExpenseSplitter;

import java.util.*;

public class MainActivity extends AppCompatActivity {

    private ExpenseDbHelper dbHelper;
    private EditText payerInput;
    private EditText amountInput;
    private EditText participantsInput;
    private TextView resultsView;
    private ListView expenseListView;
    private ArrayAdapter<String> expenseAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new ExpenseDbHelper(this);

        payerInput = findViewById(R.id.payerInput);
        amountInput = findViewById(R.id.amountInput);
        participantsInput = findViewById(R.id.participantsInput);
        resultsView = findViewById(R.id.resultsView);
        expenseListView = findViewById(R.id.expenseListView);

        Button addButton = findViewById(R.id.addExpenseButton);
        Button settleButton = findViewById(R.id.settleButton);

        expenseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        expenseListView.setAdapter(expenseAdapter);

        addButton.setOnClickListener(this::onAddExpense);
        settleButton.setOnClickListener(this::onSettle);

        refreshExpenseList();
    }

    private void onAddExpense(View view) {
        String payer = payerInput.getText().toString().trim();
        String amountStr = amountInput.getText().toString().trim();
        String participantsRaw = participantsInput.getText().toString().trim();

        if (payer.isEmpty() || amountStr.isEmpty() || participantsRaw.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Amount must be a number", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> participants = new ArrayList<>();
        for (String p : participantsRaw.split(",")) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) participants.add(trimmed);
        }
        if (!participants.contains(payer)) {
            participants.add(payer); // payer benefited too unless explicitly excluded
        }

        dbHelper.insertExpense(new ExpenseSplitter.Expense(payer, amount, participants));

        payerInput.setText("");
        amountInput.setText("");
        participantsInput.setText("");
        refreshExpenseList();
    }

    private void onSettle(View view) {
        List<ExpenseSplitter.Expense> expenses = dbHelper.getAllExpenses();
        Map<String, Double> balances = ExpenseSplitter.calculateBalances(expenses);
        List<ExpenseSplitter.Settlement> settlements = ExpenseSplitter.simplifyDebts(balances);

        StringBuilder sb = new StringBuilder();
        if (settlements.isEmpty()) {
            sb.append("Everyone is settled up! 🎉");
        } else {
            sb.append("Settle up:\n");
            for (ExpenseSplitter.Settlement s : settlements) {
                sb.append(String.format(Locale.US, "  %s → %s: $%.2f\n", s.fromPerson, s.toPerson, s.amount));
            }
        }
        resultsView.setText(sb.toString());
    }

    private void refreshExpenseList() {
        List<ExpenseSplitter.Expense> expenses = dbHelper.getAllExpenses();
        List<String> display = new ArrayList<>();
        for (ExpenseSplitter.Expense e : expenses) {
            display.add(String.format(Locale.US, "%s paid $%.2f (split %d ways)", e.payer, e.amount, e.participants.size()));
        }
        expenseAdapter.clear();
        expenseAdapter.addAll(display);
        expenseAdapter.notifyDataSetChanged();
    }
}
