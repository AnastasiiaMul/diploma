package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class ObjectsSoundsSettingsActivity extends AppCompatActivity {

    private String[] availableSounds = {"car", "person", "bicycle", "bench", "crosswalk", "Dog", "Tree", "mans hole", "Bush", "Bus stop"};
    private String[] availableSoundsUI = {"Звук 1", "Звук 2", "Звук 3", "Звук 4", "Звук 5", "Звук 6", "Звук 7", "Звук 8", "Звук 9", "Звук 10"};
    private MediaPlayer mediaPlayer;
    private String objectType;
    private static final int REQUEST_CODE_SELECT_AUDIO = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objects_sounds_settings);

        objectType = getIntent().getStringExtra("objectLabel");
        if (objectType == null) {
            finish();
            return;
        }

        LinearLayout layout = findViewById(R.id.objectSoundListLayout);

        String selectedSound = ObjectSoundPreferences.getSoundForObject(this, objectType);

        for (String sound : availableSounds) {
            Button soundButton = new Button(this);
            soundButton.setText(sound);

            if (sound.equals(selectedSound)) {
                soundButton.setBackgroundColor(Color.parseColor("#008000"));
            }

            soundButton.setOnClickListener(v -> {
                playSoundAndAskConfirmation(this, sound);
            });

            layout.addView(soundButton);
        }

        Button selectFromPhoneButton = findViewById(R.id.selectFromPhoneButton);
        selectFromPhoneButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            startActivityForResult(Intent.createChooser(intent, "Обрати аудіофайл"), REQUEST_CODE_SELECT_AUDIO);
        });

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_AUDIO && resultCode == RESULT_OK && data != null) {
            String uriString = data.getData().toString();

            // Save to preferences
            ObjectSoundPreferences.saveSoundForObject(this, objectType, uriString);

            new AlertDialog.Builder(this)
                    .setTitle("Готово")
                    .setMessage("Обрано користувацький звук:\n" + uriString)
                    .setPositiveButton("ОК", (d, w) -> finish())
                    .show();
        }
    }


    private void playSoundAndAskConfirmation(Context context, String soundName) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        int resId = context.getResources().getIdentifier(soundName.toLowerCase(), "raw", context.getPackageName());
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context, resId);
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                showConfirmationDialog(soundName); // show confirmation after sound finishes playing
            });
            mediaPlayer.start();
        } else {
            // If sound not found, immediately show confirmation
            showConfirmationDialog(soundName);
        }
        if (soundName.startsWith("content://") || soundName.startsWith("file://")) {
            try {
                mediaPlayer = MediaPlayer.create(context, Uri.parse(soundName));
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                    showConfirmationDialog(soundName);
                });
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
                showConfirmationDialog(soundName); // fallback
            }
            return;
        }

    }

    private void showConfirmationDialog(String soundName) {
        String label;
        if (soundName.startsWith("content://") || soundName.startsWith("file://")) {
            label = getFileNameFromUri(Uri.parse(soundName));
        } else {
            label = soundName;
        }

        new AlertDialog.Builder(this)
                .setTitle("Підтвердити звук")
                .setMessage("Обрати \"" + label + "\" як звук?")
                .setPositiveButton("Так", (dialog, which) -> {
                    ObjectSoundPreferences.saveSoundForObject(this, objectType, soundName);
                    finish();
                })
                .setNegativeButton("Ні", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String getFileNameFromUri(Uri uri) {
        String result = "Користувацький звук";
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex >= 0) {
                    result = cursor.getString(nameIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

}

