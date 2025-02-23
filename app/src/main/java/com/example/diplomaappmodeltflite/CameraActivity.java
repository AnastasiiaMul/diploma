package com.example.diplomaappmodeltflite;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.AspectRatioStrategy;
import androidx.camera.core.resolutionselector.ResolutionStrategy;


import org.tensorflow.lite.Interpreter;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ObjectDetectorHelper objectDetectorHelper;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;


    private static final int NUM_CLASSES = 80;
    private static final float CONFIDENCE_THRESHOLD = 0.3f; // You can adjust
    private static final int IMAGE_SIZE = 640;

    private static final float NMS_THRESHOLD = 0.5f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.cameraPreview);
        objectDetectorHelper = new ObjectDetectorHelper(this);

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (Exception e) {
                Log.e("CameraX", "Camera provider error", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .setResolutionSelector(new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(new ResolutionStrategy(
                                new Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build())
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Add ImageAnalysis to get camera frames
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(new ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(new ResolutionStrategy(
                                new Size(640, 640),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build())
                .build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::processImage);

        cameraProvider.unbindAll(); // Ensures previous bindings are cleared
        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.CAMERA}, 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length == 0 || grantResults[0] != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission denied, close the activity.
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (objectDetectorHelper != null) {
            objectDetectorHelper.close();
        }
    }

    private void processImage(ImageProxy imageProxy) {
        @SuppressLint("UnsafeOptInUsageError")
        Image image = imageProxy.getImage();

        if (image == null) {
            imageProxy.close();
            return;
        }

        Bitmap bitmap = toBitmap(image);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 640, 640, true);

        // TODO: Here will run inference
        runInference(resizedBitmap);

        imageProxy.close();
    }
    private Bitmap toBitmap(Image image) {
        YuvImage yuvImage = toYuvImage(image);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(
                new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 90, out);
        byte[] jpegBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);

        // Rotate bitmap if necessary
        Matrix matrix = new Matrix();
        matrix.postRotate(90); // Adjust rotation based on camera orientation
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private YuvImage toYuvImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // Copy Y
        yBuffer.get(nv21, 0, ySize);

        // Copy VU (NV21 format)
        int offset = ySize;
        byte[] uBytes = new byte[uSize];
        byte[] vBytes = new byte[vSize];

        vBuffer.get(vBytes);
        uBuffer.get(uBytes);

        for (int i = 0; i < vSize; i++) {
            nv21[offset++] = vBytes[i];
            nv21[offset++] = uBytes[i];
        }

        return new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
    }


    private void runInference(Bitmap bitmap) {
        Interpreter interpreter = objectDetectorHelper.getInterpreter();
        if (interpreter == null) {
            Log.e("Inference", "Interpreter not initialized.");
            return;
        }

        // Prepare input buffer [1, 640, 640, 3]
        float[][][][] inputBuffer = bitmapToInputArray(bitmap);

        // Prepare output buffer (adjust based on model's output shape)
        float[][][] outputBuffer = new float[1][84][8400]; // adjust based on YOLO model configuration

        // Run inference
        interpreter.run(inputBuffer, outputBuffer);

        List<DetectionResult> detections = parseYoloOutput(outputBuffer);
        List<DetectionResult> finalDetections = applyNMS(detections);

        for (DetectionResult det : finalDetections) {
            Log.d("NMS", "Class=" + det.detectedClass +
                    ", Conf=" + det.confidence +
                    ", Box=[" + det.left + "," + det.top + "," + det.right + "," + det.bottom + "]");
        }
    }

    private float[][][][] bitmapToInputArray(Bitmap bitmap) {
        int inputSize = 640;
        float[][][][] input = new float[1][inputSize][inputSize][3];

        for (int y = 0; y < inputSize; y++) {
            for (int x = 0; x < inputSize; x++) {
                int pixel = bitmap.getPixel(x, y);

                // Normalize pixel values to [0,1]
                input[0][y][x][0] = ((pixel >> 16) & 0xFF) / 255.0f; // R
                input[0][y][x][1] = ((pixel >> 8) & 0xFF) / 255.0f;  // G
                input[0][y][x][2] = (pixel & 0xFF) / 255.0f;         // B
            }
        }

        return input;
    }

    private List<DetectionResult> parseYoloOutput(float[][][] output) {
        List<DetectionResult> results = new ArrayList<>();

        for (int i = 0; i < output[0][0].length; i++) {
            float x = output[0][0][i];
            float y = output[0][1][i];
            float w = output[0][2][i];
            float h = output[0][3][i];

            int bestClass = -1;
            float bestConfidence = -1f;
            for (int c = 0; c < NUM_CLASSES; c++) {
                float conf = output[0][4 + c][i];
                if (conf > bestConfidence) {
                    bestConfidence = conf;
                    bestClass = c;
                }
            }

            if (bestConfidence > CONFIDENCE_THRESHOLD) {
                float left = (x - w / 2f) * IMAGE_SIZE;
                float top = (y - h / 2f) * IMAGE_SIZE;
                float right = (x + w / 2f) * IMAGE_SIZE;
                float bottom = (y + h / 2f) * IMAGE_SIZE;

                results.add(new DetectionResult(bestClass, bestConfidence, left, top, right, bottom));
            }
        }

        return results;
    }


    private List<DetectionResult> applyNMS(List<DetectionResult> detections) {
        List<DetectionResult> nmsResults = new ArrayList<>();

        detections.sort((d1, d2) -> Float.compare(d2.confidence, d1.confidence));

        while (!detections.isEmpty()) {
            DetectionResult best = detections.remove(0);
            nmsResults.add(best);

            detections.removeIf(det ->
                    det.detectedClass == best.detectedClass &&
                            best.iou(det) > NMS_THRESHOLD);
        }

        return nmsResults;
    }

}