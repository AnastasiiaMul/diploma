package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

    private List<String> fileNames;
    private boolean sortDescending = true;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyListView = findViewById(R.id.historyListView);
        Button btnToggleSort = findViewById(R.id.btnToggleSort);

        btnToggleSort.setOnClickListener(v -> {
            sortDescending = !sortDescending;
            btnToggleSort.setText(sortDescending
                    ? "Сортувати: Найновіші спочатку"
                    : "Сортувати: Найстаріші спочатку");
            loadLogFiles();
        });

        adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                new ArrayList<>());
        historyListView.setAdapter(adapter);

        historyListView.setOnItemClickListener((adapterView, view, position, id) -> {
            File selectedFile = logFiles.get(position);
            Intent intent = new Intent(HistoryActivity.this, HistoryDetailActivity.class);
            intent.putExtra("filePath", selectedFile.getAbsolutePath());
            startActivity(intent);
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadLogFiles();
    }

    private void loadLogFiles() {
        File logsDir = new File(getFilesDir(), "session_logs");
        if (!logsDir.exists()) logsDir.mkdirs();

        File[] files = logsDir.listFiles();
        logFiles = new ArrayList<>();
        fileNames = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) logFiles.add(file);
            }

            logFiles.sort((f1, f2) -> {
                long diff = f1.lastModified() - f2.lastModified();
                return sortDescending ? Long.compare(f2.lastModified(), f1.lastModified()) : Long.compare(f1.lastModified(), f2.lastModified());
            });

            for (File file : logFiles) {
                String name = file.getName().replace(".txt", "").replace(".json", "");
                try {
                    long timestamp = Long.parseLong(name);
                    String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(timestamp));
                    fileNames.add(formattedDate);
                } catch (NumberFormatException e) {
                    fileNames.add(name);
                }
            }
        }

        adapter.clear();
        adapter.addAll(fileNames);
        adapter.notifyDataSetChanged();
    }
}
