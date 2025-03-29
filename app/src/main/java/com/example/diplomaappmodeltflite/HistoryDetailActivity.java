package com.example.diplomaappmodeltflite;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class HistoryDetailActivity extends AppCompatActivity {

    private TextView textViewSessionLogs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        textViewSessionLogs = findViewById(R.id.textViewSessionLogs);

        String filePath = getIntent().getStringExtra("filePath");
        if (filePath != null) {
            File file = new File(filePath);
            StringBuilder logContent = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logContent.append(line).append("\n");
                }
            } catch (Exception e) {
                logContent.append("Failed to read log: ").append(e.getMessage());
            }

            textViewSessionLogs.setText(logContent.toString());
        } else {
            textViewSessionLogs.setText("No log file provided.");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

    }
}
