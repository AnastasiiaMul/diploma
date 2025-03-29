package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
                fileNames.add(file.getName().replace(".txt", ""));
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
