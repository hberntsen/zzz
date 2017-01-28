package hberntsen.sleeplogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.PowerManager;
import android.view.Display;

import java.io.DataOutputStream;
import java.io.IOException;

public class DisplayStateReceiver extends BroadcastReceiver {
    DataOutputStream file;

    public DisplayStateReceiver(DataOutputStream file) {
        try {
            file.writeInt(2); //version
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.file = file;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            file.writeLong(System.currentTimeMillis());
            if(Build.VERSION.SDK_INT >= 20) {
                DisplayManager dm = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
                for( Display display : dm.getDisplays()) {
                    file.writeByte(display.getState());
                }
            } else {
                //http://stackoverflow.com/a/28747907
                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                //noinspection deprecation
                file.writeByte(pm.isScreenOn() ? Display.STATE_ON : Display.STATE_OFF);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
