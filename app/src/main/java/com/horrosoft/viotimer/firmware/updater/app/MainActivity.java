package com.horrosoft.viotimer.firmware.updater.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.horrosoft.viotimer.firmware.updater.app.bluetooth.DeviceListActivity;


public class MainActivity extends Activity {

    private static final int REQUEST_BT_DEVICE_MAC_ADDRESS = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button makeAllHappyButton = (Button) findViewById(R.id.make_all_happy_button);
        final Activity activity = this;

        makeAllHappyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(activity, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_BT_DEVICE_MAC_ADDRESS);
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
        TextView tv = (TextView) findViewById(R.id.textView);
        String value = "something goes wrong";
        if (requestCode == REQUEST_BT_DEVICE_MAC_ADDRESS && resultCode == RESULT_OK) {
            value = data.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        }
        if (tv != null) {
            tv.setText(value);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
