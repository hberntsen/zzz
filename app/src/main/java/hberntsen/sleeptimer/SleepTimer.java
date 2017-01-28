package hberntsen.sleeptimer;

import android.view.Display;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by harm on 25-1-17.
 */

public class SleepTimer {
    private DataOutputStream file;
    long resetTime;

    public SleepTimer(DataOutputStream file) {
        this.file = file;
        try {
            file.writeInt(2); //version
        } catch (IOException e) {
            e.printStackTrace();
        }
        resetTime = System.currentTimeMillis();
    }

    public void reset() {
        resetTime = System.currentTimeMillis();
        try {
            file.writeLong(resetTime);
            file.writeByte(Display.STATE_ON);
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getCurrent() {
        return System.currentTimeMillis() - resetTime;
    }


}
