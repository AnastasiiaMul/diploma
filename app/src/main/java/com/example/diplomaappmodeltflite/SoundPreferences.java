package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

public class SoundPreferences {
    private static final String PREF_NAME = "sector_sounds";

    public static void saveSoundForSector(Context context, int sectorId, String soundName) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString("sector_" + sectorId, soundName).apply();
        Log.d("SoundPreferences", "Saved sound for sector " + sectorId + ": " + soundName);
    }

    public static String getSoundForSector(Context context, int sectorId) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String key = "sector_" + sectorId;
        String soundName = prefs.getString(key, null);

        Log.d("SoundPreferences", "getSoundForSector: key=" + key + ", value=" + soundName);

        return soundName;
    }

}
