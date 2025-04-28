package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private ListView historyListView;
    private List<File> logFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyListView = findViewById(R.id.historyListView);

        File logsDir = new File(getFilesDir(), "session_logs");
        if (!logsDir.exists()) {
            logsDir.mkdirs();
        }

        File[] files = logsDir.listFiles();
        logFiles = new ArrayList<>();
        List<String> fileNames = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                logFiles.add(file);
                String name = file.getName().replace(".txt", "");

                try {
                    long timestamp = Long.parseLong(name);
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
                    fileNames.add(formattedDate);
                } catch (NumberFormatException e) {
                    fileNames.add(name);
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                fileNames
        );
        historyListView.setAdapter(adapter);

        historyListView.setOnItemClickListener((adapterView, view, position, id) -> {
            File selectedFile = logFiles.get(position);
            Intent intent = new Intent(HistoryActivity.this, HistoryDetailActivity.class);
            intent.putExtra("filePath", selectedFile.getAbsolutePath());
            startActivity(intent);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

    }
}
