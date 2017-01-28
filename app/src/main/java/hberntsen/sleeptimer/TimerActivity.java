package hberntsen.sleeptimer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class TimerActivity extends AppCompatActivity {

    private TextView txtTimer;
    private GestureDetector gestureDetector;
    private Timer timer;
    private SleepTimer sleepTimer;
    private DataOutputStream sleepFile;
    private String sleepFileName;
    private Random random = new Random();

    //http://stackoverflow.com/questions/13530937/how-to-listen-to-doubletap-on-a-view-in-android/13531025#13531025
    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            resetTimer();
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            finish();
        }
    }

    private Handler updateTimeText = new Handler() {
        public void handleMessage(Message msg) {
            updateTimerText();
        }
    };
    public boolean onTouchEvent(MotionEvent e) {
        return gestureDetector.onTouchEvent(e);
    }

    private class updateScreenTask extends TimerTask {
        @Override
        public void run() {
            updateTimeText.sendEmptyMessage(0);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        gestureDetector = new GestureDetector(this, new GestureListener());
        txtTimer = (TextView)findViewById(R.id.txtTimer);

        sleepFileName = generateFileName();
        if(savedInstanceState != null) {
            sleepFileName = savedInstanceState.getString("sleepFileName");
        }
        sleepFile = openFile(sleepFileName);

        sleepTimer = new SleepTimer(sleepFile);
        if(savedInstanceState != null) {
            long resetTime = savedInstanceState.getLong("resetTime");
            if(resetTime > 0) {
                sleepTimer.resetTime = resetTime;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        timer = new Timer();
        timer.scheduleAtFixedRate(new updateScreenTask(), 0, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            sleepFile.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        timer.cancel();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putLong("resetTime", sleepTimer.resetTime);
        savedInstanceState.putString("sleepFileName", sleepFileName);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            sleepFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void resetTimer() {
        sleepTimer.reset();
        updateTimerText();

    }

    private void updateTimerText() {
        long diff = sleepTimer.getCurrent();

        long seconds = (diff / 1000) % 60 ;
        long minutes = ((diff / (1000*60)) % 60);
        long hours   = ((diff / (1000*60*60)) % 24);

        txtTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
        // anti oled burn-in
        if(seconds == 1) {
            int w = getWindowManager().getDefaultDisplay().getWidth();
            int h = getWindowManager().getDefaultDisplay().getHeight();
            int txtW = txtTimer.getWidth();
            int txtH = txtTimer.getHeight();
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)txtTimer.getLayoutParams();

            layoutParams.leftMargin = random.nextInt(w - txtW);
            layoutParams.topMargin = random.nextInt(h - txtH);
            txtTimer.setLayoutParams(layoutParams);
        }
    }

    private static String generateFileName() {
        String date = new SimpleDateFormat("y-MM-dd_HH-mm-ss").format(new Date());
        return "Log_" + date + "_resets";
    }

    private DataOutputStream openFile(String fileName) {
        try {
            return new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(new File(getExternalFilesDir(null), fileName), true)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
