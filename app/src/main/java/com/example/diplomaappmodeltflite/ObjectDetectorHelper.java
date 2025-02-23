package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
//import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class ObjectDetectorHelper {

    private static final String MODEL_PATH = "ml/yolo11n_float32.tflite";
    private Interpreter tflite;

    public ObjectDetectorHelper(Context context) {
        try {
            Interpreter.Options options = new Interpreter.Options();
            //options.addDelegate(new GpuDelegate());
            tflite = new Interpreter(loadModel(context), options);
            Log.d("TFLite", "Model loaded successfully.");
        } catch (Exception e) {
            Log.e("TFLite", "Error initializing TensorFlow Lite.", e);
        }
    }

    private MappedByteBuffer loadModel(Context context) throws Exception {
        try (AssetFileDescriptor fileDescriptor = context.getAssets().openFd(MODEL_PATH);
             FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {

            FileChannel fileChannel = inputStream.getChannel();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.getStartOffset(),
                    fileDescriptor.getDeclaredLength());
        }
    }


    public Interpreter getInterpreter() {
        return tflite;
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}