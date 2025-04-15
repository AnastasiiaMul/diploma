package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class ObjectsSettingsActivity extends AppCompatActivity {

    private LinearLayout objectsButtonsContainer;

    // ui names
    private String[] objectLabelsUI = {"Людина", "Автомобіль", "Велосипед", "Лавка", "Пішохідний перехід"};

    // internal keys
    private String[] objectLabels = {"person", "car", "bicycle", "bench", "crosswalk"};

    // Maps internal key to label and vice versa
    private final Map<String, String> objectLabelToUI = new HashMap<>();
    private Map<String, TextView> soundLabelsMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objects_settings);

        objectsButtonsContainer = findViewById(R.id.objectsButtonsContainer);

        // Create mapping between internal label and UI label
        for (int i = 0; i < objectLabels.length; i++) {
            objectLabelToUI.put(objectLabels[i], objectLabelsUI[i]);
        }

        // Create UI elements
        for (int i = 0; i < objectLabels.length; i++) {
            String internalLabel = objectLabels[i];
            String uiLabel = objectLabelsUI[i];

            // Create button with UI label
            Button objectButton = new Button(this);
            objectButton.setText(uiLabel);
            objectButton.setTextSize(20);
            objectButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, ObjectsSoundsSettingsActivity.class);
                intent.putExtra("objectLabel", internalLabel);
                startActivity(intent);
            });

            // Create label for selected sound
            TextView soundLabel = new TextView(this);
            soundLabel.setTextSize(16);
            soundLabel.setPadding(0, 4, 0, 16);

            updateSoundLabelText(internalLabel, soundLabel);
            soundLabelsMap.put(internalLabel, soundLabel);

            // Add to container
            objectsButtonsContainer.addView(objectButton);
            objectsButtonsContainer.addView(soundLabel);
        }

        // Back button
        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh sound labels when coming back from ObjectsSoundsSettingsActivity
        for (String objectLabel : objectLabels) {
            TextView label = soundLabelsMap.get(objectLabel);
            if (label != null) {
                updateSoundLabelText(objectLabel, label);
            }
        }
    }

    private void updateSoundLabelText(String objectLabel, TextView label) {
        String sound = ObjectSoundPreferences.getSoundForObject(this, objectLabel);
        if (sound == null || sound.isEmpty()) {
            sound = "Звук за замовчуванням";
        }
        label.setText("Обраний звук: " + sound);
    }
}

