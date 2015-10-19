package com.horrosoft.viotimer.firmware.updater.app;

import android.app.Activity;
import android.util.Log;
import com.horrosoft.viotimer.firmware.updater.app.bluetooth.BluetoothTools;

/**
 * Created by Kurles on 19.10.2015.
 *
 */
public class Updater {

    Activity mActivity;

    public Updater(Activity activity) {
        mActivity = activity;
    }

    public void startUpdate() {
        BluetoothTools bt = new BluetoothTools(mActivity);
        if (bt.makeBluetoothEnabled()) {
            Log.v("Timer", "ok");
        } else {
            Log.v("Timer", "bad");
        }
    }
}
