package com.example.diplomaappmodeltflite;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class SessionLogger {
    private final File logFile;
    private final JSONObject sessionData;

    private long startTimeMillis = 0;
    private long endTimeMillis = 0;
    private double startLat = 0.0, startLng = 0.0;
    private double endLat = 0.0, endLng = 0.0;
    private String endAddress = "Невідома";

    public SessionLogger(Context context) {
        String timestamp = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.US).format(new Date());
        File dir = new File(context.getFilesDir(), "session_logs");
        if (!dir.exists()) dir.mkdirs();

        logFile = new File(dir, timestamp + ".json");
        sessionData = new JSONObject();

        putSafe("timestamp", timestamp);
    }

    // --- Public Setters ---
    public void setStartTimeMillis(long millis) {
        this.startTimeMillis = millis;
    }

    public void setEndTimeMillis(long millis) {
        this.endTimeMillis = millis;
    }

    public void setStartCoordinates(double lat, double lng) {
        this.startLat = lat;
        this.startLng = lng;
    }

    public void setEndCoordinates(double lat, double lng) {
        this.endLat = lat;
        this.endLng = lng;
    }

    public void setEndAddress(String address) {
        this.endAddress = (address != null && !address.isEmpty()) ? address : "Невідома";
    }

    public void setSnapshotPath(String path) {
        putSafe("snapshotPath", path);
    }

    public void setDistanceKm(double distanceKm) {
        putSafe("distanceKm", distanceKm);
    }

    public void setDuration(String duration) {
        putSafe("duration", duration);
    }

    public void setStartLocation(String startLocation) {
        putSafe("startLocationName", startLocation);
    }

    public void setEndLocation(String endLocation) {
        putSafe("endLocationName", endLocation);
    }

    public void log(String line) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(line).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Finalization ---
    public void finalizeLog() {
        try {
            putSafe("startTimeMillis", startTimeMillis);
            putSafe("endTimeMillis", endTimeMillis);
            putSafe("startLat", startLat);
            putSafe("startLng", startLng);
            putSafe("endLat", endLat);
            putSafe("endLng", endLng);
            putSafe("endAddress", endAddress);

            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write(sessionData.toString(4));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getLogFile() {
        return logFile;
    }

    // --- Private helper ---
    private void putSafe(String key, Object value) {
        try {
            sessionData.put(key, value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}