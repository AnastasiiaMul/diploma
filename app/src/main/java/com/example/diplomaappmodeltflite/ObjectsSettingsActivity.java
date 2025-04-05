package com.example.diplomaappmodeltflite;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class ObjectsSettingsActivity extends AppCompatActivity {
    private String[] objectTypes = {"Person", "Car", "Bicycle", "Bench"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_objects_settings);

        LinearLayout layout = findViewById(R.id.objectButtonsLayout);

        for (String obj : objectTypes) {
            Button btn = new Button(this);
            btn.setText(obj);
            btn.setTextSize(24f);
            btn.setOnClickListener(v -> {
                Intent intent = new Intent(this, ObjectsSoundsSettingsActivity.class);
                intent.putExtra("objectType", obj);
                startActivity(intent);
            });
            layout.addView(btn);
        }

        Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());
    }
}

