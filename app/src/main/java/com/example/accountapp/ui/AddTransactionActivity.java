package com.example.accountapp.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.accountapp.R;
import com.example.accountapp.db.DatabaseHelper;
import com.example.accountapp.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AddTransactionActivity extends AppCompatActivity {
    private RadioGroup rgType;
    private RadioButton rbIncome, rbExpense;
    private EditText etAmount, etNote;
    private Spinner spinnerCategory;
    private Button btnDate, btnSave;
    private DatabaseHelper dbHelper;
    private Calendar selectedDate;
    private String[] incomeCategories = {"Salary", "Bonus", "Investment", "Gift", "Other"};
    private String[] expenseCategories = {"Food", "Shopping", "Transport", "Entertainment", "Bills", "Healthcare", "Rent", "Other"};
    private long editingTransactionId = -1; // -1 means new transaction, > 0 means editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);

        // Enable back button in ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        initViews();
        setupCategorySpinner();
        setupDatePicker();
        setupSaveButton();
        setupTypeToggle();

        // Check if we're editing an existing transaction
        editingTransactionId = getIntent().getLongExtra("transaction_id", -1);
        if (editingTransactionId > 0) {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Edit Transaction");
            }
            // Update save button text
            btnSave.setText("Update Transaction");
            loadTransactionForEdit(editingTransactionId);
        } else {
            // If type was passed from MainActivity, set it
            String transactionType = getIntent().getStringExtra("transaction_type");
            if (transactionType != null) {
                if ("Income".equals(transactionType)) {
                    rbIncome.setChecked(true);
                } else {
                    rbExpense.setChecked(true);
                }
            }
        }
    }
    
    private void loadTransactionForEdit(long transactionId) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final Transaction transaction = dbHelper.getTransactionById(transactionId);
                if (transaction != null) {
                    // Update UI on main thread
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            // Set transaction type
                            if ("Income".equals(transaction.getType())) {
                                rbIncome.setChecked(true);
                            } else {
                                rbExpense.setChecked(true);
                            }
                            
                            // Set amount
                            etAmount.setText(String.format(Locale.getDefault(), "%.2f", transaction.getAmount()));
                            
                            // Set category
                            updateCategorySpinner();
                            String[] categories = "Income".equals(transaction.getType()) ? incomeCategories : expenseCategories;
                            for (int i = 0; i < categories.length; i++) {
                                if (categories[i].equals(transaction.getCategory())) {
                                    spinnerCategory.setSelection(i);
                                    break;
                                }
                            }
                            
                            // Set date
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                java.util.Date date = sdf.parse(transaction.getDate());
                                if (date != null) {
                                    selectedDate.setTime(date);
                                    updateDateButton();
                                }
                            } catch (java.text.ParseException e) {
                                e.printStackTrace();
                                // If parsing fails, keep current date
                            }
                            
                            // Set note
                            if (transaction.getNote() != null && !transaction.getNote().isEmpty()) {
                                etNote.setText(transaction.getNote());
                            }
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        rgType = findViewById(R.id.rg_type);
        rbIncome = findViewById(R.id.rb_income);
        rbExpense = findViewById(R.id.rb_expense);
        etAmount = findViewById(R.id.et_amount);
        etNote = findViewById(R.id.et_note);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnDate = findViewById(R.id.btn_date);
        btnSave = findViewById(R.id.btn_save);
        dbHelper = new DatabaseHelper(this);
        selectedDate = Calendar.getInstance();
        
        // Setup amount input to handle decimal points from both keyboard and screen
        setupAmountInput();
    }
    
    private void setupAmountInput() {
        // Set input type to allow decimal numbers - this enables decimal point on both screen and physical keyboard
        etAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        
        // Add filter to normalize decimal separators and prevent multiple decimal points
        etAmount.setFilters(new InputFilter[] {
            new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end,
                                           Spanned dest, int dstart, int dend) {
                    if (source.length() == 0) {
                        return null; // Keep existing text
                    }
                    
                    StringBuilder builder = new StringBuilder();
                    String destStr = dest.toString();
                    boolean hasDecimalPoint = destStr.contains(".");
                    
                    for (int i = start; i < end; i++) {
                        char c = source.charAt(i);
                        
                        // Replace comma with dot (for keyboards that use comma as decimal separator)
                        if (c == ',') {
                            c = '.';
                        }
                        
                        // Allow digits
                        if (Character.isDigit(c)) {
                            builder.append(c);
                        }
                        // Allow decimal point only if there isn't one already
                        else if (c == '.' && !hasDecimalPoint) {
                            builder.append(c);
                            hasDecimalPoint = true;
                        }
                        // Reject other characters
                    }
                    
                    return builder.length() > 0 ? builder : null;
                }
            }
        });
    }

    private void setupCategorySpinner() {
        updateCategorySpinner();
    }

    private void updateCategorySpinner() {
        String[] categories;
        if (rbIncome.isChecked()) {
            categories = incomeCategories;
        } else {
            categories = expenseCategories;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupTypeToggle() {
        rgType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                updateCategorySpinner();
            }
        });
    }

    private void setupDatePicker() {
        updateDateButton();
        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        AddTransactionActivity.this,
                        new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                                selectedDate.set(year, month, dayOfMonth);
                                updateDateButton();
                            }
                        },
                        selectedDate.get(Calendar.YEAR),
                        selectedDate.get(Calendar.MONTH),
                        selectedDate.get(Calendar.DAY_OF_MONTH)
                );
                datePickerDialog.show();
            }
        });
    }

    private void updateDateButton() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        btnDate.setText(sdf.format(selectedDate.getTime()));
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTransaction();
            }
        });
    }

    private void saveTransaction() {
        String amountStr = etAmount.getText().toString().trim();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Normalize decimal separator (replace comma with dot)
        amountStr = amountStr.replace(',', '.');
        
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                Toast.makeText(this, "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
            return;
        }

        String type = rbIncome.isChecked() ? "Income" : "Expense";
        String category = spinnerCategory.getSelectedItem().toString();
        String note = etNote.getText().toString().trim();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = sdf.format(selectedDate.getTime());

        // Save in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                Transaction transaction;
                if (editingTransactionId > 0) {
                    // Update existing transaction
                    transaction = new Transaction(editingTransactionId, type, amount, category, date, note);
                    dbHelper.updateTransaction(transaction);
                } else {
                    // Create new transaction
                    transaction = new Transaction(type, amount, category, date, note);
                    dbHelper.addTransaction(transaction);
                }
                
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("is_edit", editingTransactionId > 0);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    }
                });
            }
        }).start();
    }
}

