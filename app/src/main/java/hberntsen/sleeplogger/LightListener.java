package hberntsen.sleeplogger;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by harm on 1-9-16.
 */
public class LightListener implements SensorEventListener {
    DataOutputStream file;
    long lastTimeStamp = 0;

    public LightListener(DataOutputStream file) {
        this.file = file;
    }

    public void onSensorChanged(SensorEvent event) {
        final long second = 1000000000;
        long timeDiff = event.timestamp - lastTimeStamp;
        if(timeDiff > 10*second) {
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
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
