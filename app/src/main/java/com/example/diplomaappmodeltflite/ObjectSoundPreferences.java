package com.example.diplomaappmodeltflite;

import android.content.Context;

public class ObjectSoundPreferences {
    private static final String PREF_NAME = "object_sounds";

    public static void saveSoundForObject(Context context, String objectType, String soundName) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(objectType.toLowerCase(), soundName)
                .apply();
    }

    public static String getSoundForObject(Context context, String objectType) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getString(objectType.toLowerCase(), null);
    }
}

