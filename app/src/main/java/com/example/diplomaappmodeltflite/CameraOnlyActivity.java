package com.example.diplomaappmodeltflite;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;

public class CameraOnlyActivity extends AppCompatActivity {

    private PreviewView cameraPreview;
    private OverlayView overlayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_only);

        cameraPreview = findViewById(R.id.cameraPreviewOnly);
        overlayView = findViewById(R.id.overlayViewOnly);

        Button backBtn = findViewById(R.id.btnBack);
        backBtn.setOnClickListener(v -> finish());

        // Use same binding logic as in CameraActivity
        CameraUtils.bindCameraPreview(this, cameraPreview, overlayView);
    }
}

