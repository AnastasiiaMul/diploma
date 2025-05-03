package com.example.diplomaappmodeltflite;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.widget.Toast;

public class CompassManager implements SensorEventListener {
    public interface CompassListener {
        void onAzimuthChanged(float azimuth);
    }

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final Sensor magnetometer;
    private final Context context;
    private CompassListener listener;

    private float[] gravity;
    private float[] geomagnetic;

    public CompassManager(Context context) {
        this.context = context;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void setCompassListener(CompassListener listener) {
        this.listener = listener;
    }

    public void startListening() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
    }

    public void stopListening() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            gravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            geomagnetic = event.values;

        if (gravity != null && geomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];

            boolean success = SensorManager.getRotationMatrix(R, I, gravity, geomagnetic);
            if (success) {
                float[] orientation = new float[3];
                SensorManager.getOrientation(R, orientation);
                float azimuth = (float) Math.toDegrees(orientation[0]);

                if (azimuth < 0) azimuth += 360;

                if (listener != null) {
                    listener.onAzimuthChanged(azimuth);
                }

                String direction = getCompassDirection(azimuth);
                //Toast.makeText(context, direction, Toast.LENGTH_SHORT).show();
                playCompassSound(direction);
                stopListening();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private String getCompassDirection(float azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) return "north";
        else if (azimuth >= 22.5 && azimuth < 67.5) return "ne";
        else if (azimuth >= 67.5 && azimuth < 112.5) return "east";
        else if (azimuth >= 112.5 && azimuth < 157.5) return "se";
        else if (azimuth >= 157.5 && azimuth < 202.5) return "south";
        else if (azimuth >= 202.5 && azimuth < 247.5) return "sw";
        else if (azimuth >= 247.5 && azimuth < 292.5) return "west";
        else return "nw";
    }

    private void playCompassSound(String direction) {
        int resId = context.getResources().getIdentifier(direction, "raw", context.getPackageName());
        if (resId != 0) {
            MediaPlayer mediaPlayer = MediaPlayer.create(context, resId);
            mediaPlayer.setOnCompletionListener(MediaPlayer::release);
            mediaPlayer.start();
        }
    }
}