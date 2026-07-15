package com.example.accountapp.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.accountapp.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        // Enable back button in ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        TextView tvAbout = findViewById(R.id.tv_about);
        tvAbout.setText("CashFlow\n\n" +
                "Version: 1.0\n\n" +
                "A simple and intuitive mobile application to help you easily record daily income and expenses, " +
                "and clearly understand your spending habits through visual charts.\n\n" +
                "Features:\n" +
                "• Record income and expenses\n" +
                "• View financial overview\n" +
                "• Statistics with pie charts\n" +
                "• Multiple categories\n" +
                "• Date-based filtering\n\n" +
                "© 2025 CashFlow");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

