package com.example.diplomaappmodeltflite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

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

                    // Timestamp formatting
                    long timestampMillis = session.optLong("timestamp", 0);
                    String formattedTimestamp = timestampMillis > 0
                            ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(timestampMillis))
                            : "Unknown";

                    formatted.append("Початок: ").append(formattedTimestamp).append("\n\n");
                    formatted.append("Звідки: ").append(session.optString("startLocationName", "Невідомо")).append("\n");
                    formatted.append("Куди: ").append(session.optString("endLocationName", "Невідомо")).append("\n\n");

                    formatted.append("Запланована дистанція: ").append(session.optDouble("plannedDistanceKm", 0.0)).append(" km\n");
                    formatted.append("Пройдена дистанція: ").append(session.optDouble("distanceKm", 0.0)).append(" km\n\n");

                    formatted.append("Тривалість: ").append(session.optString("duration", "Невідомо")).append("\n\n");

                    // Legs display
                    JSONArray legsArray = session.optJSONArray("legs");
                    if (legsArray != null && legsArray.length() > 0) {
                        formatted.append("Етапи маршруту:\n");
                        for (int i = 0; i < legsArray.length(); i++) {
                            JSONObject leg = legsArray.getJSONObject(i);
                            long seconds = leg.optLong("seconds", 0);
                            double meters = leg.optDouble("meters", 0.0);
                            formatted.append(" - Leg ").append(i + 1)
                                    .append(": Time = ").append(seconds).append("s, Distance = ").append(meters / 1000.0).append(" km\n");
                        }
                        formatted.append("\n");
                    }

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
                    textViewSessionLogs.setText("Невалідна сесія");
                }

            } catch (Exception e) {
                textViewSessionLogs.setText("Невдалося зчитати логи сесії:\n" + e.getMessage());
            }

        } else {
            textViewSessionLogs.setText("Не надано файлу логування.");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }
}
