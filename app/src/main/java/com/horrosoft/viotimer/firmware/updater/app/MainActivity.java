package com.horrosoft.viotimer.firmware.updater.app;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.horrosoft.viotimer.firmware.updater.app.bluetooth.DeviceListActivity;
import com.horrosoft.viotimer.firmware.updater.app.bluetooth.FirmwareReader;
import com.horrosoft.viotimer.firmware.updater.app.bluetooth.IUploadStatusListener;
import com.horrosoft.viotimer.firmware.updater.app.bluetooth.UploadFirmwareProtocol;
import com.lamerman.FileDialog;
import com.lamerman.SelectionMode;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;


public class MainActivity extends Activity implements IUploadStatusListener {

    private static final int REQUEST_BT_DEVICE_MAC_ADDRESS = 0;
    private static final int LOAD_FILE_ID = 1;

    private FirmwareReader firmwareReader = null;
    private ProgressDialog progress = null;
    private UploadFirmwareProtocol uploadFirmwareProtocol = null;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button makeAllHappyButton = (Button) findViewById(R.id.make_all_happy_button);
        Button loadFirmwareButton = (Button) findViewById(R.id.load_button);
        final Activity activity = this;

        makeAllHappyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_BT_DEVICE_MAC_ADDRESS);
            }
        });

        loadFirmwareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getBaseContext(), FileDialog.class);
                intent.putExtra(FileDialog.START_PATH, Environment.getExternalStorageDirectory().getPath());
                intent.putExtra(FileDialog.SELECTION_MODE, SelectionMode.MODE_CREATE);

                //can user select directories or not
                intent.putExtra(FileDialog.CAN_SELECT_DIR, false);

                //alternatively you can set file filter
                intent.putExtra(FileDialog.FORMAT_FILTER, new String[]{"tmr"});
                startActivityForResult(intent, LOAD_FILE_ID);

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            return;
        }
        if (requestCode == REQUEST_BT_DEVICE_MAC_ADDRESS) {
            if (resultCode == RESULT_OK) {
                String value = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                uploadFirmwareProtocol = new UploadFirmwareProtocol(value, firmwareReader, this);
                uploadFirmwareProtocol.start();

            }
        } else if (requestCode == LOAD_FILE_ID && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FileDialog.RESULT_PATH);
            File file = new File(filePath);
            byte[] firmware = new byte[(int) file.length()];
            try {
                DataInputStream ds = new DataInputStream((new FileInputStream(file)));
                ds.readFully(firmware);
                firmwareReader = new FirmwareReader(firmware);
                if (firmwareReader.isValid()) {
                    setDescriptionText(firmwareReader.getDescription());
                    setResultText("firmware loaded");
                } else {
                    setDescriptionText("");
                    setResultText("wrong firmware file");
                    firmwareReader = null;
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setResultText(String text) {
        TextView tv = (TextView) findViewById(R.id.textViewOperationResult);
        tv.setText(text);
    }

    private void setDescriptionText(String text) {
        TextView tv = (TextView) findViewById(R.id.textViewDescription);
        tv.setText(text);
    }

    private void setMessageOnProgressBarAndCloseAfter(String message, int delay) {
        if (progress != null) {
            progress.setMessage(message);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (progress != null) {
                        progress.dismiss();
                        progress = null;
                    }
                }
            }, delay);
        }
    }

    @Override
    public void error(String errorMessage) {
        if (progress != null) {
            progress.setTitle("Error.");
        }
        setMessageOnProgressBarAndCloseAfter(errorMessage, 2000);
        setResultText(errorMessage);
        uploadFirmwareProtocol = null;
    }

    @Override
    public void uploadProgress(int current, int total) {
        if (progress == null) {
            progress = new ProgressDialog(this);
            progress.setTitle("Flashing...");
            progress.setMessage("Please wait");
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setIndeterminate(false);
            progress.setMax(total);
            progress.setCancelable(false);
            progress.setCanceledOnTouchOutside(false);
            progress.show();
        }

        progress.setProgress(current);
        progress.setSecondaryProgress(current);
    }

    @Override
    public void finishUpload() {
        if (progress != null) {
            progress.setTitle("Upload finished.");
        }
        String finalMessage = "Upload finished.";
        setMessageOnProgressBarAndCloseAfter(finalMessage, 2000);
        setResultText(finalMessage);
        uploadFirmwareProtocol = null;

    }
}
