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
    private File logFile;
    private final JSONObject sessionData;

    public SessionLogger(Context context) {
        String timestamp = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.US).format(new Date());
        File dir = new File(context.getFilesDir(), "session_logs");
        if (!dir.exists()) dir.mkdirs();
        logFile = new File(dir, timestamp + ".json");
        sessionData = new JSONObject();
        try {
            sessionData.put("timestamp", timestamp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void log(String line) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(line).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setSnapshotPath(String path) {
        try {
            sessionData.put("snapshotPath", path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDistanceKm(double distanceKm) {
        try {
            sessionData.put("distanceKm", distanceKm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDuration(String duration) {
        try {
            sessionData.put("duration", duration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setStartLocation(String startLocation) {
        try {
            sessionData.put("startLocationName", startLocation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setEndLocation(String endLocation) {
        try {
            sessionData.put("endLocationName", endLocation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finalizeLog() {
        try (FileWriter writer = new FileWriter(logFile)) {
            writer.write(sessionData.toString(4));
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public File getLogFile() {
        return logFile;
    }
}