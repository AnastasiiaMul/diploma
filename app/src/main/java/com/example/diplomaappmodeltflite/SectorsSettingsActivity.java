package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class SectorsSettingsActivity extends AppCompatActivity {

    private EditText numSectorsInput;
    private LinearLayout sectorsButtonsContainer;

    // Mapping system sounds to UI names
    private static final Map<String, String> soundUIMap = new HashMap<>();
    static {
        soundUIMap.put("sector1", "До");
        soundUIMap.put("sector2", "Ре");
        soundUIMap.put("sector3", "Мі");
        soundUIMap.put("sector4", "Фа");
        soundUIMap.put("sector5", "Соль");
        soundUIMap.put("sector6", "Ля");
        soundUIMap.put("sector7", "Сі");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sectors_settings);

        numSectorsInput = findViewById(R.id.numSectorsInput);
        sectorsButtonsContainer = findViewById(R.id.sectorsButtonsContainer);

        Button generateButtons = findViewById(R.id.generateButtons);
        Button backButton = findViewById(R.id.backButton);

        generateButtons.setOnClickListener(v -> {
            String inputText = numSectorsInput.getText().toString();
            if (!inputText.isEmpty()) {
                int numSectors = Integer.parseInt(inputText);
                SectorSoundManager.setNumberOfSectors(this, numSectors); // Save to SharedPreferences
                generateSectorButtons(numSectors);
            }
        });
        backButton.setOnClickListener(v -> finish());
    }

    private void generateSectorButtons(int numSectors) {
        sectorsButtonsContainer.removeAllViews();

        for (int i = 1; i <= numSectors; i++) {
            int sectorId = i;

            // Create button
            Button sectorButton = new Button(this);
            sectorButton.setText("Сектор " + sectorId);
            sectorButton.setTextSize(20);
            sectorButton.setOnClickListener(v -> {
                Intent intent = new Intent(SectorsSettingsActivity.this, SectorsSoundSettingsActivity.class);
                intent.putExtra("sectorId", sectorId);
                startActivity(intent);
            });

            // Create label for selected sound
            TextView soundLabel = new TextView(this);
            soundLabel.setTextSize(16);
            soundLabel.setPadding(0, 4, 0, 16);

            String savedSound = SectorSoundManager.getSoundForSector(this, sectorId);
            String uiFriendlyName = getUiNameForSound(savedSound);

            if (uiFriendlyName == null) {
                soundLabel.setText("Обраний звук: Звук за замовчуванням");
            } else {
                soundLabel.setText("Обраний звук: " + uiFriendlyName);
            }

            // Add to layout
            sectorsButtonsContainer.addView(sectorButton);
            sectorsButtonsContainer.addView(soundLabel);
        }
    }

    private String getUiNameForSound(String systemSoundName) {
        if (systemSoundName == null) return null;

        if (soundUIMap.containsKey(systemSoundName)) {
            return soundUIMap.get(systemSoundName);
        }

        if ((systemSoundName.startsWith("content://") || systemSoundName.startsWith("file://"))) {
            Uri uri = Uri.parse(systemSoundName);
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        return cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return "Користувацький звук";
        }

        return systemSoundName; // fallback
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Load saved number of sectors from preferences
        int savedNumSectors = SectorSoundManager.getNumberOfSectors(this);
        if (savedNumSectors > 0) {
            numSectorsInput.setText(String.valueOf(savedNumSectors));
            generateSectorButtons(savedNumSectors);
        }
    }
}



