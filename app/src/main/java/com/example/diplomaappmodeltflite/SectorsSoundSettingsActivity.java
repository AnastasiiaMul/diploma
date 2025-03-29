package com.example.diplomaappmodeltflite;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SectorsSoundSettingsActivity extends AppCompatActivity {
    private ListView soundListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sectors_sound_settings);

        int sectorId = getIntent().getIntExtra("sectorId", -1);
        TextView sectorLabel = findViewById(R.id.sectorLabel);
        sectorLabel.setText("Оберіть звук для сектору " + sectorId);

        soundListView = findViewById(R.id.soundListView);
        String[] sounds = {"Beep", "Ping", "Buzz", "Alarm"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, sounds);
        soundListView.setAdapter(adapter);

        soundListView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSound = sounds[position];
            // TODO: Store sound selection for this sector
            Toast.makeText(this, "Обрано " + selectedSound + " для сектору " + sectorId, Toast.LENGTH_SHORT).show();
        });

        findViewById(R.id.backToSectors).setOnClickListener(v -> finish());
    }
}

