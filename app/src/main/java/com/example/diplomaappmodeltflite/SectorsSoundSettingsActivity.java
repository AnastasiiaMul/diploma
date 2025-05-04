package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import java.util.HashMap;
import java.util.Map;

public class SectorsSoundSettingsActivity extends AppCompatActivity {
    private int sectorId;
    private String[] availableSoundsUI = {"До", "Ре", "Мі", "Фа", "Соль", "Ля", "Сі"};
    private String[] availableSounds = {"sector1", "sector2", "sector3", "sector4", "sector5", "sector6", "sector7"};

    private MediaPlayer mediaPlayer;

    private static final int PICK_AUDIO_REQUEST = 2001;
    private LinearLayout soundListLayout;

    private final Map<String, String> soundUIMap = new HashMap<String, String>() {{
        put("sector1", "До");
        put("sector2", "Ре");
        put("sector3", "Мі");
        put("sector4", "Фа");
        put("sector5", "Соль");
        put("sector6", "Ля");
        put("sector7", "Сі");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sectors_sound_settings);

        // Get the sector ID from intent
        sectorId = getIntent().getIntExtra("sectorId", 1);

        Button selectFromPhoneBtn = findViewById(R.id.selectFromPhoneButton);
        LinearLayout soundListLayout = findViewById(R.id.soundListLayout);

        // Load current selected sound for this sector
        String selectedSound = SoundPreferences.getSoundForSector(this, sectorId);

        // Dynamically add buttons for each sound
        for (int i = 0; i < availableSounds.length; i++) {
            String soundSystemName = availableSounds[i];
            String soundUIName = availableSoundsUI[i];

            Button soundButton = new Button(this);
            soundButton.setText(soundUIName);

            if (soundSystemName.equals(selectedSound)) {
                soundButton.setBackgroundColor(Color.parseColor("#008000"));
            }

            soundButton.setOnClickListener(v -> {
                playSoundAndAskConfirmation(this, soundSystemName, soundUIName);
            });
            soundListLayout.addView(soundButton);
        }

        selectFromPhoneBtn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("audio/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, PICK_AUDIO_REQUEST);
        });

        // Back button
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_AUDIO_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException e) {
                Log.e("URI_PERMISSION", "Failed to take persistable URI permission", e);
            }
            String uriString = uri.toString();
            String fileName = getFileNameFromUri(uri);

            playSelectedUserSound(uriString, fileName);
        }
    }

    private void playSelectedUserSound(String uriString, String fileName) {
        try {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = MediaPlayer.create(this, Uri.parse(uriString));
            if (mediaPlayer == null) throw new Exception("MediaPlayer.create() returned null");

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                confirmAndSaveUriSound(uriString, fileName);
            });
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e("USER_SOUND", "Failed to play user-selected sound", e);
            confirmAndSaveUriSound(uriString, fileName);
        }
    }

    private void confirmAndSaveUriSound(String uriString, String fileName) {
        new AlertDialog.Builder(this)
                .setTitle("Підтвердити звук")
                .setMessage("Обрати \"" + fileName + "\" як звук?")
                .setPositiveButton("Так", (dialog, which) -> {
                    SoundPreferences.saveSoundForSector(this, sectorId, uriString);
                    finish();
                })
                .setNegativeButton("Ні", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void playSoundAndAskConfirmation(Context context, String systemSoundName, String uiSoundName) {
        if (mediaPlayer != null) mediaPlayer.release();

        int resId = context.getResources().getIdentifier(systemSoundName.toLowerCase(), "raw", context.getPackageName());
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(context, resId);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                    showConfirmationDialog(systemSoundName, uiSoundName);
                });
                mediaPlayer.start();
            } else {
                showConfirmationDialog(systemSoundName, uiSoundName);
            }
        } else {
            showConfirmationDialog(systemSoundName, uiSoundName);
        }
    }

    private void showConfirmationDialog(String systemSoundName, String uiSoundName) {
        new AlertDialog.Builder(this)
                .setTitle("Підтвердити звук")
                .setMessage("Обрати \"" + uiSoundName + "\" як звук?")
                .setPositiveButton("Так", (dialog, which) -> {
                    SoundPreferences.saveSoundForSector(this, sectorId, systemSoundName);
                    finish();
                })
                .setNegativeButton("Ні", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }

    private String getFileNameFromUri(Uri uri) {
        DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
        return documentFile != null ? documentFile.getName() : "Користувацький звук";
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

