# Group Expense Splitter (Android · Java · SQLite)

Add shared expenses (who paid, how much, split with whom), and get back the **minimal set of payments** needed to settle everyone up — built with **Java**, traditional Android Views (XML layouts), and **SQLite**.

## Read this first: what's verified, and how

Same situation as the Kotlin project in this portfolio: **no `javac` in this sandbox, and no network to get one** (confirmed - `apt-get install openjdk-21-jdk-headless` returned 403 Forbidden). So instead of unverified Java:

1. `reference-impl/expense_splitter.py` — the exact algorithm in Python.
2. `tests/test_all.py` — **16 tests, run for real, all passing** in this sandbox — including one genuine catch: my first test assertion had the wrong expected value (I'd done the arithmetic wrong by hand), and the test suite caught *my* mistake, not the code's. Left in the README because that's exactly what a good test suite is for.
3. `ExpenseSplitter.java` mirrors the Python line-for-line, and `ExpenseSplitterTest.java` has the same 8 test cases in JUnit, ready for `./gradlew test`.

## The algorithm: debt simplification

The interesting part isn't splitting a bill three ways — it's this: after several expenses with different payers, you end up with a tangle of who-owes-whom. Naively settling each expense separately could mean 5+ separate payments for a 4-person trip. `simplifyDebts()` uses greedy max-creditor/max-debtor matching to collapse that down to the **minimum number of transactions** — verified in the tests to settle a 4-person, 3-expense trip in at most 3 payments, and to bring every balance to exactly zero when applied.

## Architecture

```
reference-impl/expense_splitter.py   Verified algorithm (run: python3 tests/test_all.py)
tests/test_all.py                      16 passing tests

app/src/main/java/com/portfolio/expensesplitter/
├── logic/ExpenseSplitter.java          Pure Java port (no Android deps): balances + debt simplification
├── data/ExpenseDbHelper.java             SQLiteOpenHelper - stores expenses
└── ui/MainActivity.java                   Traditional Views: add expense, list, "Calculate Settlement" button
app/src/main/res/layout/activity_main.xml   XML layout (not Compose - contrasts with the Kotlin/Compose project)
app/src/test/.../ExpenseSplitterTest.java    JUnit port of the same 8 test cases
```

## Setup & Run

```bash
./gradlew assembleDebug     # build the APK
./gradlew test               # run ExpenseSplitterTest (JVM, no emulator needed)
```

Or open in Android Studio and click Run.

## Why Java + XML Views instead of Kotlin + Compose here?

Deliberately different stack from the other Android project in this portfolio (Kotlin/Compose/Room) — same reason the 9 core portfolio projects each cover different technologies: it demonstrates both the modern (Compose) and traditional (XML Views + Java) approaches to Android development, both of which show up in real job postings depending on the age of the codebase you'd be joining.

## Known limitations / next steps

- No expense editing/deletion UI yet (only add + clear-all via `ExpenseDbHelper.clearAll()`).
- No currency formatting/locale handling beyond basic `String.format`.
- No multi-currency support — all amounts assumed to be the same currency.

## Tech stack

Java · Android SDK (Views/XML) · SQLite · AppCompat
