package com.example.diplomaappmodeltflite;

import android.content.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
public class SessionLogger {
    private File logFile;

    public SessionLogger(Context context) {
        String timestamp = new SimpleDateFormat("MM-dd-yyyy_HH-mm-ss", Locale.US).format(new Date());
        File dir = new File(context.getFilesDir(), "session_logs");
        if (!dir.exists()) dir.mkdirs();
        logFile = new File(dir, timestamp + ".txt");
    }

    public void log(String line) {
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(line).append("\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getLogFile() {
        return logFile;
    }
}