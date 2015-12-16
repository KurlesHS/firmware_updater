package com.horrosoft.viotimer.firmware.updater.app.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * Created by Alexey on 15.12.2015.
 *
 */


public class UploadFirmwareProtocol {

    public final static int UPLOAD_FINISHED = 1;
    public final static int ERROR_FINISHED = 2;
    public final static int UPLOAD_PROGRESS = 3;

    private static class MyHandler extends Handler {

        //Using a weak reference means you won't prevent garbage collection
        private final WeakReference<UploadFirmwareProtocol> myClassWeakReference;

        public MyHandler(UploadFirmwareProtocol myClassInstance) {
            super(Looper.getMainLooper());
            myClassWeakReference = new WeakReference<UploadFirmwareProtocol>(myClassInstance);
        }

        @Override
        public void handleMessage(Message msg) {
            UploadFirmwareProtocol myClass = myClassWeakReference.get();
            if (myClass != null) {
                myClass.handleMessage(msg);
            }
            super.handleMessage(msg);
        }
    }


    private class BluetoothThreadWorker  extends Thread {
        private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        private String mDeviceMacAddress;
        private MyHandler mHandler;
        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothSocket mBtSocket = null;
        private boolean connectionStatus = true;
        private OutputStream mOutStream = null;
        private InputStream mInputStream = null;
        private FirmwareReader firmwareReader;

        public BluetoothThreadWorker(String deviceMacAddress,FirmwareReader firmwareReader, MyHandler handler) {
            this.firmwareReader = firmwareReader;
            mHandler = handler;
            mDeviceMacAddress = deviceMacAddress;
            mBluetoothAdapter = null;

        }

        @Override
        public void run() {
            try {
                mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceMacAddress);

                // We need two things before we can successfully connect
                // (authentication issues aside): a MAC address, which we
                // already have, and an RFCOMM channel.
                // Because RFCOMM channels (aka ports) are limited in
                // number, Android doesn't allow you to use them directly;
                // instead you request a RFCOMM mapping based on a service
                // ID. In our case, we will use the well-known SPP Service
                // ID. This ID is in UUID (GUID to you Microsofties)
                // format. Given the UUID, Android will handle the
                // mapping for you. Generally, this will return RFCOMM 1,
                // but not always; it depends what other BlueTooth services
                // are in use on your Android device.
                try {
                    mBtSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                } catch (IOException e) {
                    connectionStatus = false;
                }
            }catch (IllegalArgumentException e) {
                connectionStatus = false;
        }
            mBluetoothAdapter.cancelDiscovery();

            try {
                mBtSocket.connect();
            } catch (IOException e1) {
                connectionStatus = false;
                try {
                    mBtSocket.close();
                } catch (IOException e2) {
                    connectionStatus = false;
                }
            }

            try {
                mOutStream = mBtSocket.getOutputStream();
                mInputStream = mBtSocket.getInputStream();
            } catch (IOException e2) {
                connectionStatus = false;
            }

            if (!connectionStatus) {
                mHandler.obtainMessage(ERROR_FINISHED, "error connect to device");
                return;
            }

            try {
                byte [] buffer = new byte[1024];
                //mOutStream.write("hello!\n".getBytes());
                mHandler.obtainMessage(UPLOAD_PROGRESS, 0, 0).sendToTarget();

                for (;;) {
                    int read = readTimeout(buffer, 2000);
                    if (read < 0) {
                        mHandler.obtainMessage(ERROR_FINISHED, "error happens while flashing =(").sendToTarget();
                        break;
                    }
                    if (read > 0) {
                        mOutStream.write(buffer, 0, read);
                        if (buffer[0] == 'q') {
                            break;
                        }
                    } else {
                        mOutStream.write("error 0".getBytes());
                    }
                }
                mHandler.obtainMessage(UPLOAD_FINISHED).sendToTarget();
            } catch (IOException e) {
                e.printStackTrace();
                mHandler.obtainMessage(ERROR_FINISHED, "error happens while flashing =(").sendToTarget();
            }
        }

        int readTimeout(byte[] buffer, int timeout) throws IOException {
            int retVal = 0;
            int currentTimeout = 0;
            while (true) {
                if (mInputStream.available() > 0) {
                    retVal = mInputStream.read(buffer);
                    break;
                }
                int sleepPeriod = 200;
                int remainSleepPeriod = timeout - currentTimeout;
                if (remainSleepPeriod <= 0) {
                    break;
                } else if (remainSleepPeriod < sleepPeriod) {
                    sleepPeriod = remainSleepPeriod;
                }
                try {
                    Thread.sleep(sleepPeriod);
                } catch (InterruptedException e) {
                    break;
                }
                currentTimeout += 200;
            }
            return retVal;
        }

    }

    private IUploadStatusListener statusListener;

    public UploadFirmwareProtocol(String deviceMacAddress, FirmwareReader firmwareReader, IUploadStatusListener statusListener) {
        this.statusListener = statusListener;
        MyHandler handler = new MyHandler(this);
        BluetoothThreadWorker threadWorker = new BluetoothThreadWorker(deviceMacAddress, firmwareReader, handler);
        threadWorker.start();
    }

    public void handleMessage(Message msg) {
        switch (msg.what) {
            case ERROR_FINISHED:
                statusListener.error(msg.obj.toString());
                break;
            case UPLOAD_FINISHED:
                statusListener.finishUpload();
                break;
            case UPLOAD_PROGRESS:
                statusListener.uploadProgress(msg.arg1, msg.arg2);
                break;
            default:
                break;
        }
    }

}
