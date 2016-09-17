package hberntsen.sleeplogger;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by harm on 1-9-16.
 */
public class AccelerometerListener implements SensorEventListener {
    DataOutputStream file;

    public AccelerometerListener(DataOutputStream file) {
        this.file = file;
    }

    public void onSensorChanged(SensorEvent event) {
        try {
            file.writeLong(System.currentTimeMillis());
            file.writeLong(event.timestamp);
            for (int i = 0; i < event.values.length; i++) {
                file.writeFloat(event.values[i]);
            }
        } catch (IOException e) {
             e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
