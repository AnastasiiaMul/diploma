package com.example.diplomaappmodeltflite;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CameraUtils {

    public static Bitmap toBitmap(Image image, int rotationDegrees) {
        YuvImage yuvImage = toYuvImage(image);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(
                new Rect(0, 0, yuvImage.getWidth(), yuvImage.getHeight()), 90, out);
        byte[] jpegBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.length);

        Matrix matrix = new Matrix();
        //matrix.postRotate(90); // Adjust rotation if needed
        matrix.postRotate(rotationDegrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private static YuvImage toYuvImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];
        yBuffer.get(nv21, 0, ySize);

        byte[] uBytes = new byte[uSize];
        byte[] vBytes = new byte[vSize];
        vBuffer.get(vBytes);
        uBuffer.get(uBytes);

        int offset = ySize;
        for (int i = 0; i < vSize; i++) {
            nv21[offset++] = vBytes[i];
            nv21[offset++] = uBytes[i];
        }

        return new YuvImage(nv21, ImageFormat.NV21, image.getWidth(), image.getHeight(), null);
    }
    public static float[][][][] bitmapToInputArray(Bitmap bitmap) {
        int inputSize = 640; //changed to 320 from 640
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
}
