package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SectorsSettingsActivity extends AppCompatActivity {

    private EditText numSectorsInput;
    private LinearLayout sectorsButtonsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sectors_settings);

        numSectorsInput = findViewById(R.id.numSectorsInput);
        sectorsButtonsContainer = findViewById(R.id.sectorsButtonsContainer);

        Button generateButtons = findViewById(R.id.generateButtons);
        Button backButton = findViewById(R.id.backButton);

        generateButtons.setOnClickListener(v -> generateSectorButtons());
        backButton.setOnClickListener(v -> finish());
    }

    private void generateSectorButtons() {
        sectorsButtonsContainer.removeAllViews();
        String inputText = numSectorsInput.getText().toString();
        if (inputText.isEmpty()) return;

        int numSectors = Integer.parseInt(inputText);
        SectorSoundManager.setNumberOfSectors(this, numSectors);

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
            String sound = SectorSoundManager.getSoundForSector(this, sectorId);
            if (sound == null || sound.isEmpty()) {
                sound = "Звук за замовчуванням";
            }
            soundLabel.setText("Обраний звук: " + sound);

            // Add to layout
            sectorsButtonsContainer.addView(sectorButton);
            sectorsButtonsContainer.addView(soundLabel);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Regenerate buttons with updated sound labels when returning from sound selection
        generateSectorButtons();
    }
}



