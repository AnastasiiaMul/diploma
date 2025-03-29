package com.example.diplomaappmodeltflite;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryManager {

    private static final String HISTORY_DIR = "history";

    public static void saveLog(Context context, String log) {
        String timestamp = new SimpleDateFormat("MM_dd_yyyy_HH_mm", Locale.getDefault()).format(new Date());
        File file = new File(context.getFilesDir(), HISTORY_DIR);
        if (!file.exists()) file.mkdir();

        File sessionFile = new File(file, timestamp + ".txt");
        try (FileWriter writer = new FileWriter(sessionFile)) {
            writer.write(log);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getAllSessionNames(Context context) {
        File file = new File(context.getFilesDir(), HISTORY_DIR);
        if (!file.exists()) return new ArrayList<>();

        List<String> sessions = new ArrayList<>();
        for (File f : file.listFiles()) {
            sessions.add(f.getName().replace(".txt", ""));
        }
        return sessions;
    }

    public static String getLogsForSession(Context context, String sessionName) {
        File file = new File(context.getFilesDir(), HISTORY_DIR + "/" + sessionName + ".txt");
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content.toString();
    }
}
