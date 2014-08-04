package com.ktind.cgm.bgscout;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.ktind.cgm.bgscout.R;

public class MainActivity extends Activity {
    private static final String TAG = DeviceDownloadService.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button button= (Button) findViewById(R.id.button);
        if (isServiceRunning()) {
            button.setText("Start");
        } else {
            button.setText("Stop");
        }

    }

    public void toggleService(View view){
        Intent mIntent=new Intent(MainActivity.this,DeviceDownloadService.class);
        final Button button= (Button) findViewById(R.id.button);
        if (isServiceRunning()) {
            Log.d(TAG, "Stopping service");
            stopService(mIntent);
            button.setText("Start");
        } else {
            Log.d(TAG, "Starting service");
            startService(mIntent);
            button.setText("Stop");
        }
    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.ktind.cgm.bgscout.DeviceDownloadService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
