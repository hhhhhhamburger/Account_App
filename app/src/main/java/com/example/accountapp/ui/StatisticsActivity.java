package com.example.accountapp.ui;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.accountapp.R;
import com.example.accountapp.db.DatabaseHelper;
import com.example.accountapp.model.Transaction;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {
    private PieChart pieChart;
    private Spinner spinnerPeriod;
    private DatabaseHelper dbHelper;
    private String[] periods = {"This Week", "This Month", "This Year", "All Time"};
    private LinearLayout legendLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Enable back button in ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        initViews();
        // Clear any existing data first
        if (pieChart != null) {
            pieChart.clear();
        }
        if (legendLayout != null) {
            legendLayout.removeAllViews();
        }
        setupPeriodSpinner();
        // Load "This Week" data immediately
        loadStatistics("This Week");
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
        pieChart = findViewById(R.id.pie_chart);
        spinnerPeriod = findViewById(R.id.spinner_period);
        legendLayout = findViewById(R.id.legend_layout);
        dbHelper = new DatabaseHelper(this);
        
        // Initialize chart settings
        if (pieChart != null) {
            pieChart.setUsePercentValues(true); // Enable percentage display
            pieChart.getDescription().setEnabled(false);
            pieChart.setExtraOffsets(5, 10, 5, 5);
            pieChart.setDragDecelerationFrictionCoef(0.95f);
            pieChart.setDrawHoleEnabled(false);
            pieChart.setRotationEnabled(true);
            pieChart.setHighlightPerTapEnabled(true);
            // Disable legend on chart (we'll use custom legend)
            pieChart.getLegend().setEnabled(false);
            // Hide value text on chart slices
            pieChart.setDrawEntryLabels(false);
        }
    }

    private void setupPeriodSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, periods);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPeriod.setAdapter(adapter);
        
        // Set default selection to "This Week" (index 0)
        spinnerPeriod.setSelection(0, false); // false means don't trigger listener

        spinnerPeriod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadStatistics(periods[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void loadStatistics(String period) {
        // Clear chart and legend first on main thread to avoid showing stale data
        if (pieChart != null) {
            pieChart.clear();
            pieChart.invalidate();
        }
        if (legendLayout != null) {
            legendLayout.removeAllViews();
        }
        
        // Load data in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String[] dateRange = getDateRange(period);
                    String startDate = dateRange[0];
                    String endDate = dateRange[1];

                    List<Transaction> transactions = dbHelper.getTransactionsByDateRange(startDate, endDate);
                    
                    // Calculate expenses by category
                    Map<String, Double> categoryMap = new HashMap<>();
                    String[] categories = {"Food", "Shopping", "Transport", "Entertainment", "Bills", "Healthcare", "Rent", "Other"};
                    
                    // Only calculate expenses (not income)
                    for (String category : categories) {
                        try {
                            double amount = dbHelper.getExpenseByCategory(category, startDate, endDate);
                            if (amount > 0) {
                                categoryMap.put(category, amount);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    // Check if there are any expenses in the date range
                    double totalExpense = dbHelper.getTotalExpense(startDate, endDate);
                    if (totalExpense <= 0) {
                        categoryMap.clear(); // Clear map if no expenses
                    }

                    // Update chart on main thread
                    final Map<String, Double> finalMap = new HashMap<>(categoryMap);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            updatePieChart(finalMap);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    // Show error on main thread
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (pieChart != null) {
                                pieChart.clear();
                                pieChart.setNoDataText("Error loading data");
                                pieChart.setNoDataTextColor(android.graphics.Color.RED);
                                pieChart.invalidate();
                            }
                            if (legendLayout != null) {
                                legendLayout.removeAllViews();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private String[] getDateRange(String period) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String endDate;
        String startDate;

        if ("This Week".equals(period)) {
            // Get Monday of current week
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
            calendar.add(Calendar.DAY_OF_YEAR, -daysFromMonday);
            startDate = sdf.format(calendar.getTime());
            
            // Get Sunday of current week
            calendar.add(Calendar.DAY_OF_YEAR, 6);
            endDate = sdf.format(calendar.getTime());
        } else if ("This Month".equals(period)) {
            // First day of current month
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            startDate = sdf.format(calendar.getTime());
            
            // Last day of current month
            calendar = Calendar.getInstance();
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            endDate = sdf.format(calendar.getTime());
        } else if ("This Year".equals(period)) {
            // First day of current year
            calendar.set(Calendar.DAY_OF_YEAR, 1);
            startDate = sdf.format(calendar.getTime());
            
            // Last day of current year
            calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, Calendar.DECEMBER);
            calendar.set(Calendar.DAY_OF_MONTH, 31);
            endDate = sdf.format(calendar.getTime());
        } else { // All Time
            calendar.set(2000, 0, 1); // Set to year 2000 as start
            startDate = sdf.format(calendar.getTime());
            calendar = Calendar.getInstance();
            endDate = sdf.format(calendar.getTime());
        }

        return new String[]{startDate, endDate};
    }

    private void updatePieChart(Map<String, Double> categoryMap) {
        try {
            if (pieChart == null) {
                return;
            }
            
            if (categoryMap == null || categoryMap.isEmpty()) {
                pieChart.clear();
                pieChart.setNoDataText("No expense data available");
                pieChart.setNoDataTextColor(android.graphics.Color.GRAY);
                pieChart.invalidate();
                if (legendLayout != null) {
                    legendLayout.removeAllViews();
                }
                return;
            }

            // Calculate total for percentage calculation
            double total = 0;
            for (Double value : categoryMap.values()) {
                total += value;
            }

            List<PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Double> entry : categoryMap.entrySet()) {
                double value = entry.getValue();
                if (value > 0) {
                    entries.add(new PieEntry((float) value, entry.getKey()));
                }
            }

            if (entries.isEmpty()) {
                pieChart.clear();
                pieChart.setNoDataText("No expense data available");
                pieChart.setNoDataTextColor(android.graphics.Color.GRAY);
                pieChart.invalidate();
                if (legendLayout != null) {
                    legendLayout.removeAllViews();
                }
                return;
            }

            PieDataSet dataSet = new PieDataSet(entries, "Expense Categories");
            int[] colors = ColorTemplate.COLORFUL_COLORS;
            dataSet.setColors(colors);
            dataSet.setSliceSpace(2f);
            dataSet.setSelectionShift(5f);
            // Hide value text on chart
            dataSet.setDrawValues(false);

            PieData pieData = new PieData(dataSet);
            // Hide all text on chart
            pieData.setDrawValues(false);
            
            pieChart.setData(pieData);
            pieChart.getDescription().setEnabled(false);
            pieChart.setCenterText("Expense Statistics");
            pieChart.setCenterTextSize(16f);
            pieChart.setCenterTextColor(android.graphics.Color.BLACK);
            pieChart.setUsePercentValues(true);
            pieChart.animateY(1000);
            pieChart.invalidate();
            
            // Update custom legend with colors and percentages
            updateLegend(categoryMap, entries, colors, total);
        } catch (Exception e) {
            e.printStackTrace();
            if (pieChart != null) {
                pieChart.clear();
                pieChart.setNoDataText("Error loading chart data");
                pieChart.setNoDataTextColor(android.graphics.Color.RED);
                pieChart.invalidate();
            }
            if (legendLayout != null) {
                legendLayout.removeAllViews();
            }
        }
    }
    
    private void updateLegend(Map<String, Double> categoryMap, List<PieEntry> entries, int[] colors, double total) {
        if (legendLayout == null) {
            return;
        }
        
        // Clear existing legend items
        legendLayout.removeAllViews();
        
        // Create legend items for each category
        for (int i = 0; i < entries.size(); i++) {
            PieEntry entry = entries.get(i);
            String category = entry.getLabel();
            double value = entry.getValue();
            double percentage = (value / total) * 100;
            int color = colors[i % colors.length];
            
            // Create horizontal layout for each legend item
            LinearLayout itemLayout = new LinearLayout(this);
            itemLayout.setOrientation(LinearLayout.HORIZONTAL);
            itemLayout.setPadding(0, 4, 0, 4);
            
            // Color indicator (square)
            View colorView = new View(this);
            int size = (int) (32 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams colorParams = new LinearLayout.LayoutParams(size, size);
            colorParams.setMargins(0, 0, 8, 0);
            colorView.setLayoutParams(colorParams);
            colorView.setBackgroundColor(color);
            
            // Text view for category name and percentage
            TextView textView = new TextView(this);
            textView.setText(category + " " + String.format(Locale.getDefault(), "%.1f%%", percentage));
            textView.setTextSize(14);
            textView.setTextColor(Color.BLACK);
            
            itemLayout.addView(colorView);
            itemLayout.addView(textView);
            
            legendLayout.addView(itemLayout);
        }
    }
}

