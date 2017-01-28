package hberntsen.sleeplogger;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


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

    private DisplayStateReceiver displayStateReceiver;
    private DataOutputStream displayFile;

    final static private boolean recordLight = false;

    private Sensor getDefaultSensor(int type) {
        // android-to do: need to be smarter, for now, just return the 1st sensor
        List<Sensor> l = sensorManager.getSensorList(type);
        return l.isEmpty() ? null : l.get(0);
    }

    @Override
    public void onCreate() {
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        lightSensor = getDefaultSensor(Sensor.TYPE_LIGHT);
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

            Notification notification = new NotificationCompat.Builder(getApplicationContext())
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

            if(recordLight) {
                lightFile = openFile("light");
                lightListener = new LightListener(lightFile);
                sensorManager.registerListener(lightListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
            }

            displayFile = openFile("screen");
            displayStateReceiver = new DisplayStateReceiver(displayFile);
            registerReceiver(displayStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
            registerReceiver(displayStateReceiver, new IntentFilter(Intent.ACTION_SCREEN_ON));
        }

        return START_STICKY;
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private DataOutputStream openFile(String tag) {
        String date = new SimpleDateFormat("y-MM-dd_HH-mm-ss").format(new Date());
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
        if(recordLight) {
            sensorManager.unregisterListener(lightListener);
        }
        unregisterReceiver(displayStateReceiver);

        try {
            accelerometerFile.close();
            if(recordLight) {
                lightFile.close();
            }
            displayFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        running = false;
    }
}
