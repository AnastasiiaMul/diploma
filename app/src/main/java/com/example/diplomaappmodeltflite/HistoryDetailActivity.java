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
    private ImageView imageViewSnapshot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history_detail);

        textViewSessionLogs = findViewById(R.id.textViewSessionLogs);
        imageViewSnapshot = findViewById(R.id.imageViewSnapshot);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        String filePath = getIntent().getStringExtra("filePath");
        if (filePath != null) {
            displaySessionLog(new File(filePath));
        } else {
            textViewSessionLogs.setText("Не надано файл логування.");
        }
    }

    private void displaySessionLog(File file) {
        StringBuilder jsonBuilder = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }

            JSONObject session = new JSONObject(jsonBuilder.toString().trim());
            StringBuilder formatted = new StringBuilder();

            // Dates
            long startMillis = session.optLong("startTimeMillis", 0);
            long endMillis = session.optLong("endTimeMillis", 0);
            String formattedStart = formatDate(startMillis);
            String formattedEnd = formatDate(endMillis);

            formatted.append("Час початку подорожі: ").append(formattedStart).append("\n");
            formatted.append("Час кінця подорожі: ").append(formattedEnd).append("\n\n");

            // Locations
            formatted.append("Звідки: ").append(session.optString("startLocationName", "Невідомо")).append("\n");
            formatted.append("Куди: ").append(session.optString("endLocationName", "Невідомо")).append("\n\n");

            // Distance & Duration
            formatted.append("Запланована дистанція: ")
                    .append(String.format(Locale.US, "%.2f", session.optDouble("plannedDistanceKm", 0.0)))
                    .append(" km\n");

            formatted.append("Пройдена дистанція: ")
                    .append(String.format(Locale.US, "%.2f", session.optDouble("distanceKm", 0.0)))
                    .append(" km\n\n");

            formatted.append("Тривалість подорожі: ")
                    .append(session.optString("duration", "Невідомо"))
                    .append("\n\n");

            // Coordinates
            formatted.append("Координати старту: ")
                    .append(session.optDouble("startLat", 0)).append(", ")
                    .append(session.optDouble("startLng", 0)).append("\n");

            formatted.append("Координати кінця: ")
                    .append(session.optDouble("endLat", 0)).append(", ")
                    .append(session.optDouble("endLng", 0)).append("\n");

            formatted.append("Адреса кінця: ")
                    .append(session.optString("endAddress", "Невідома")).append("\n\n");

            // Route legs
            JSONArray legsArray = session.optJSONArray("legs");
            if (legsArray != null && legsArray.length() > 0) {
                formatted.append("Етапи маршруту:\n");
                for (int i = 0; i < legsArray.length(); i++) {
                    JSONObject leg = legsArray.getJSONObject(i);
                    long seconds = leg.optLong("seconds", 0);
                    double meters = leg.optDouble("meters", 0.0);
                    formatted.append(" - Етап ").append(i + 1)
                            .append(": Час = ").append(seconds).append(" сек, Дистанція = ")
                            .append(String.format(Locale.US, "%.2f", meters / 1000.0)).append(" км\n");
                }
                formatted.append("\n");
            }

            textViewSessionLogs.setText(formatted.toString());

            // Load snapshot
            displaySnapshot(session.optString("snapshotPath", null));

        } catch (Exception e) {
            textViewSessionLogs.setText("Невдалося зчитати логи сесії:\n" + e.getMessage());
        }
    }

    private String formatDate(long millis) {
        return (millis > 0)
                ? new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date(millis))
                : "Невідомо";
    }

    private void displaySnapshot(String snapshotPath) {
        if (snapshotPath == null) return;

        File imageFile = new File(snapshotPath);
        if (imageFile.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            imageViewSnapshot.setImageBitmap(bitmap);
            imageViewSnapshot.setVisibility(View.VISIBLE);
        }
    }
}
