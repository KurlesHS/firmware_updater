package com.horrosoft.viotimer.firmware.updater.app.bluetooth;

import android.content.Intent;

import java.util.List;

/**
 * Created by Kurles on 19.10.2015.
 *
 */
public interface IActivityResultListener {
    void onActivityResultHandler(int requestCode, int resultCode, Intent data);
    public List<Integer> supportedRequestCodes();
}
