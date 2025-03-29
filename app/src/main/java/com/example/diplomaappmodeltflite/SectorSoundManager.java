package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.content.SharedPreferences;

import android.content.Context;
import android.content.SharedPreferences;

public class SectorSoundManager {

    private static final String PREF_NAME = "sector_sounds";

    public static void setSoundForSector(Context context, int sectorId, String soundName) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString("sector_" + sectorId, soundName)
                .apply();
    }

    public static String getSoundForSector(Context context, int sectorId) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString("sector_" + sectorId, "");
    }

    public static void setNumberOfSectors(Context context, int num) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt("num_sectors", num)
                .apply();
    }

    public static int getNumberOfSectors(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt("num_sectors", 3);  // Default = 3 sectors
    }
}


