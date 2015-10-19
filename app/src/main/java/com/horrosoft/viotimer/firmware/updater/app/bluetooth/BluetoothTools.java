package com.horrosoft.viotimer.firmware.updater.app.bluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;

/**
 * Created by Kurles on 19.10.2015.
 *
 */
public class BluetoothTools {

    private static final int REQUEST_ENABLE_BT = 0;

    private Activity mActivity;


    private BluetoothAdapter adapter;

    public BluetoothTools(Activity activity) {
        mActivity = activity;
        adapter = BluetoothAdapter.getDefaultAdapter();

    }
    public boolean isBlueToothPresent() {
        return adapter != null;
    }

    public boolean makeBluetoothEnabled() {
        boolean retVal = false;
        if (isBlueToothPresent()) {
            if (adapter.isEnabled()) {
                retVal = true;
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                retVal = adapter.isEnabled();
            }
        }
        return  retVal;
    }
}
