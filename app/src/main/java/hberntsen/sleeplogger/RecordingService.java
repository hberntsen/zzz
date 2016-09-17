package hberntsen.sleeplogger;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;


/**
 * Created by harm on 1-9-16.
 */
public class RecordingService extends Service {
    final int ONGOING_NOTIFICATION_ID = 1234;
    public static boolean running = false;
    SensorManager sensorManager;


    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private Sensor accelerometerSensor;
    private AccelerometerListener accelerometerListener;
    private DataOutputStream accelerometerFile;

    private Sensor lightSensor;
    private LightListener lightListener;
    private DataOutputStream lightFile;


    @Override
    public void onCreate() {
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, false);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT, false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!running) {
            running = true;
            Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification.Builder(getApplicationContext())
                    .setContentTitle("Sleeplogger is recording...")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(contentIntent)
                    .build();
            startForeground(ONGOING_NOTIFICATION_ID, notification);

            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SleepLogger - RecordingService");
            wakeLock.acquire();

            accelerometerFile = openFile("accelerometer");
            accelerometerListener = new AccelerometerListener(accelerometerFile);
            sensorManager.registerListener(accelerometerListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

            lightFile = openFile("light");
            lightListener = new LightListener(lightFile);
            sensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        return START_STICKY;
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private DataOutputStream openFile(String tag) {
        String date = new Date().toString();
        //return new File(Environment.getExternalStorageDirectory(), ftag+ntag);
        try {
            return new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(new File(getExternalFilesDir(null), "Log_" + date + "_" + tag))));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }



    @Override
    public void onDestroy() {
        wakeLock.release();
        sensorManager.unregisterListener(accelerometerListener);
        sensorManager.unregisterListener(lightListener);

        try {
            accelerometerFile.close();
            lightFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
        return;
    }
}
