package com.example.diplomaappmodeltflite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

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
        ImageView imageViewSnapshot = findViewById(R.id.imageViewSnapshot);

        String filePath = getIntent().getStringExtra("filePath");
        if (filePath != null) {
            File file = new File(filePath);
            StringBuilder builder = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }

                String content = builder.toString().trim();
                if (content.startsWith("{")) {
                    JSONObject session = new JSONObject(content);
                    StringBuilder formatted = new StringBuilder();

                    formatted
                    .append("Час подорожі: ").append(session.getString("timestamp")).append("\n")
                            .append("З: ").append(session.getString("startLocationName")).append("\n")
                            .append("До: ").append(session.getString("endLocationName")).append("\n")
                            .append("Відстань: ").append(session.getDouble("distanceKm")).append(" km\n")
                            .append("Тривалість: ").append(session.getString("duration")).append("\n");
                    textViewSessionLogs.setText(formatted.toString());

                    // Load and display snapshot
                    String snapshotPath = session.optString("snapshotPath", null);
                    if (snapshotPath != null) {
                        File imageFile = new File(snapshotPath);
                        if (imageFile.exists()) {
                            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                            imageViewSnapshot.setImageBitmap(bitmap);
                            imageViewSnapshot.setVisibility(View.VISIBLE);
                        }
                    }

                } else {
                    textViewSessionLogs.setText("⚠️ Not a valid session log.");
                }

            } catch (Exception e) {
                textViewSessionLogs.setText("Failed to read session log:\n" + e.getMessage());
            }

        } else {
            textViewSessionLogs.setText("No log file provided.");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
