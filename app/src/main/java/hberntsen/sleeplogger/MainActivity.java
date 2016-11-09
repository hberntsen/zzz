package hberntsen.sleeplogger;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    Button startButton;
    Button stopButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startButton = (Button) findViewById(R.id.startbutton);
        stopButton = (Button) findViewById(R.id.stopbutton);

        final Intent intent = new Intent(this, RecordingService.class);
        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startService(intent);
                updateButtons(true);
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopService(intent);
                updateButtons(false);
            }
        });
        updateButtons();

    }
    private void updateButtons() {
        updateButtons(RecordingService.running);
    }

    private void updateButtons(boolean running) {
        if (running) {
            stopButton.setEnabled(true);
            startButton.setEnabled(false);
        } else {
            stopButton.setEnabled(false);
            startButton.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateButtons();
    }
}

