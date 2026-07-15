package com.example.accountapp.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.accountapp.R;
import com.example.accountapp.db.DatabaseHelper;
import com.example.accountapp.model.Transaction;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private TextView tvTotalIncome, tvTotalExpense, tvBalance;
    private RecyclerView recyclerView;
    private TransactionAdapter adapter;
    private DatabaseHelper dbHelper;
    private FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupFloatingActionButton();
        
        dbHelper = new DatabaseHelper(this);
        loadFinancialData();
    }

    private void initViews() {
        tvTotalIncome = findViewById(R.id.tv_total_income);
        tvTotalExpense = findViewById(R.id.tv_total_expense);
        tvBalance = findViewById(R.id.tv_balance);
        recyclerView = findViewById(R.id.recycler_view_transactions);
        fab = findViewById(R.id.fab);
    }

    private void setupRecyclerView() {
        adapter = new TransactionAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        
        // Set click listener for transactions
        adapter.setOnTransactionClickListener(new TransactionAdapter.OnTransactionClickListener() {
            @Override
            public void onTransactionClick(Transaction transaction) {
                // Open AddTransactionActivity in edit mode
                Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
                intent.putExtra("transaction_id", transaction.getId());
                startActivityForResult(intent, 100);
            }

            @Override
            public void onTransactionLongClick(Transaction transaction) {
                // Show delete confirmation dialog
                showDeleteConfirmationDialog(transaction);
            }
        });
    }
    
    private void showDeleteConfirmationDialog(final Transaction transaction) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure you want to delete this transaction?\n\n" +
                        "Type: " + transaction.getType() + "\n" +
                        "Amount: $" + String.format(Locale.getDefault(), "%.2f", transaction.getAmount()) + "\n" +
                        "Category: " + transaction.getCategory())
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteTransaction(transaction);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteTransaction(final Transaction transaction) {
        // Delete in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean deleted = dbHelper.deleteTransaction(transaction.getId());
                
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (deleted) {
                            Toast.makeText(MainActivity.this, "Transaction deleted successfully!", Toast.LENGTH_SHORT).show();
                            loadFinancialData(); // Refresh the list
                        } else {
                            Toast.makeText(MainActivity.this, "Failed to delete transaction", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }).start();
    }

    private void setupFloatingActionButton() {
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTransactionMenu();
            }
        });
    }

    private void showAddTransactionMenu() {
        // Create a custom dialog with larger text
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_transaction, null);
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setTitle("Add Transaction");
        
        AlertDialog dialog = builder.create();
        
        TextView tvIncome = dialogView.findViewById(R.id.tv_income);
        TextView tvExpense = dialogView.findViewById(R.id.tv_expense);
        
        tvIncome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
                intent.putExtra("transaction_type", "Income");
                startActivityForResult(intent, 100);
            }
        });
        
        tvExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Intent intent = new Intent(MainActivity.this, AddTransactionActivity.class);
                intent.putExtra("transaction_type", "Expense");
                startActivityForResult(intent, 100);
            }
        });
        
        dialog.show();
    }

    private void loadFinancialData() {
        // Load data in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.DAY_OF_MONTH, 1);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String startDate = sdf.format(calendar.getTime());
                
                calendar = Calendar.getInstance();
                String endDate = sdf.format(calendar.getTime());
                
                final double totalIncome = dbHelper.getTotalIncome(startDate, endDate);
                final double totalExpense = dbHelper.getTotalExpense(startDate, endDate);
                final double balance = totalIncome - totalExpense;
                
                final List<Transaction> transactions = dbHelper.getAllTransactions();
                
                // Update UI on main thread
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        tvTotalIncome.setText(String.format(Locale.getDefault(), "$%.2f", totalIncome));
                        tvTotalExpense.setText(String.format(Locale.getDefault(), "$%.2f", totalExpense));
                        tvBalance.setText(String.format(Locale.getDefault(), "$%.2f", balance));
                        
                        adapter.updateTransactions(transactions);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadFinancialData();
            boolean isEdit = data != null && data.getBooleanExtra("is_edit", false);
            if (isEdit) {
                Toast.makeText(this, "Transaction updated successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Transaction saved successfully!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_statistics) {
            Intent intent = new Intent(this, StatisticsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.menu_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

