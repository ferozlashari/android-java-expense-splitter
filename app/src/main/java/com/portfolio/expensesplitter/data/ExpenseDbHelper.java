package com.portfolio.expensesplitter.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.portfolio.expensesplitter.logic.ExpenseSplitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExpenseDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "expenses.db";
    private static final int DB_VERSION = 1;

    private static final String TABLE_EXPENSES = "expenses";
    private static final String COL_ID = "id";
    private static final String COL_PAYER = "payer";
    private static final String COL_AMOUNT = "amount";
    private static final String COL_PARTICIPANTS = "participants"; // comma-separated

    public ExpenseDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_EXPENSES + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_PAYER + " TEXT NOT NULL, " +
            COL_AMOUNT + " REAL NOT NULL, " +
            COL_PARTICIPANTS + " TEXT NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_EXPENSES);
        onCreate(db);
    }

    public long insertExpense(ExpenseSplitter.Expense expense) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PAYER, expense.payer);
        values.put(COL_AMOUNT, expense.amount);
        values.put(COL_PARTICIPANTS, String.join(",", expense.participants));
        return db.insert(TABLE_EXPENSES, null, values);
    }

    public List<ExpenseSplitter.Expense> getAllExpenses() {
        List<ExpenseSplitter.Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_EXPENSES, null, null, null, null, null, COL_ID + " DESC");

        try {
            while (cursor.moveToNext()) {
                String payer = cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYER));
                double amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_AMOUNT));
                String participantsRaw = cursor.getString(cursor.getColumnIndexOrThrow(COL_PARTICIPANTS));
                List<String> participants = Arrays.asList(participantsRaw.split(","));
                expenses.add(new ExpenseSplitter.Expense(payer, amount, participants));
            }
        } finally {
            cursor.close();
        }
        return expenses;
    }

    public void clearAll() {
        getWritableDatabase().delete(TABLE_EXPENSES, null, null);
    }
}
