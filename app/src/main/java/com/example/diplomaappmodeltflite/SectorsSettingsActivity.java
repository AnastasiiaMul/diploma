package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class SectorsSettingsActivity extends AppCompatActivity {
    private LinearLayout sectorsButtonsContainer;
    private EditText numSectorsInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sectors_settings);

        numSectorsInput = findViewById(R.id.numSectorsInput);
        sectorsButtonsContainer = findViewById(R.id.sectorsButtonsContainer);

        findViewById(R.id.generateButtons).setOnClickListener(v -> {
            sectorsButtonsContainer.removeAllViews();
            int num = Integer.parseInt(numSectorsInput.getText().toString());

            for (int i = 1; i <= num; i++) {
                Button button = new Button(this);
                button.setText("Сектор " + i);
                int finalI = i;
                button.setOnClickListener(view -> {
                    Intent intent = new Intent(this, SectorsSoundSettingsActivity.class);
                    intent.putExtra("sectorId", finalI);
                    startActivity(intent);
                });
                sectorsButtonsContainer.addView(button);
            }
        });

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }
}


