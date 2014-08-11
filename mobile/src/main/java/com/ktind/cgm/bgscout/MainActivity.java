package com.ktind.cgm.bgscout;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean mBounded=false;
    private DeviceDownloadService mServer;
    private Handler mHandler=new Handler();
    private ArrayList<UIDevice> UIDeviceList=new ArrayList<UIDevice>();

    private ArrayList<String> deviceList=new ArrayList<String>();
    private AlarmReceiver alarmReceiver;
//    private boolean serviceAvailable;

    public class UIDevice{
        private ProgressBar progressBar;
        private TextView bgValue;
        private TextView name;
        private String deviceID;
        private AbstractDevice device;

        UIDevice(ProgressBar p,TextView b,TextView n,String d){
            setProgressBar(p);
            setBgValue(b);
            setName(n);
            setDeviceID(d);
            progressBar.setVisibility(View.VISIBLE);
            bgValue.setVisibility(View.VISIBLE);
            name.setVisibility(View.VISIBLE);
            bgValue.setText("---");
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            name.setText(sharedPref.getString(getDeviceID()+"_name","Null"));
        }

        public void setDevice(AbstractDevice cgm){
            device=cgm;
            name.setText(cgm.getName());
        }

        public AbstractDevice getDevice(){
            return device;
        }

        public String getDeviceID() {
            return deviceID;
        }

        public void setDeviceID(String deviceID) {
            this.deviceID = deviceID;
        }

        public ProgressBar getProgressBar() {
            return progressBar;
        }

        public void setProgressBar(ProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        public TextView getBgValue() {
            return bgValue;
        }

        public void setBgValue(TextView bgValue) {
            this.bgValue = bgValue;
        }

        public TextView getName() {
            return name;
        }

        public void setName(TextView name) {
            Log.d(TAG,"Setting UI name to "+name);
            this.name = name;
        }

        public void setBGDisplay(int sgv,Trend trend){
            setBGDisplay(String.valueOf(sgv)+" "+trend.toString());
        }

        public void setBGDisplay(String text){
            Log.d(TAG,"Setting UI bgDisplay to "+text);
            bgValue.setText(text);
        }

        public void update(){
            long nextReading = mServer.getDeviceNextReading(getDeviceID()) / 1000L;
//            long pollInterval = mServer.getPollInterval(getDeviceID());
//            String deviceName = mServer.getDeviceName(getDeviceID());
//            Log.v(TAG,device.getName()+" has a poll interval of "+device.getPollInterval()+" ms");
            Log.v(TAG,"Next reading: "+nextReading);
            getProgressBar().setProgress((int) Math.abs(nextReading));
            if (getProgressBar().getProgress() == device.getPollInterval()) {
                getProgressBar().setProgress(0);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        // Look into dynamically creating these?
        ProgressBar progressBar = null;
        TextView bg = null;
        TextView name = null;
        String deviceIDStr;
        alarmReceiver=new AlarmReceiver();
        registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.UI_READING_UPDATE"));
        registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.SERVICE_READY"));
        for (int i=1;i<5;i++){
            if (sharedPref.getBoolean("device_" + i + "_enable", false)) {
                deviceIDStr="device_"+i;
                deviceList.add(deviceIDStr);
                switch (i) {
                    case (1):
                        progressBar = (ProgressBar) findViewById(R.id.progressBar);
                        bg = (TextView) findViewById(R.id.textView);
                        name = (TextView) findViewById(R.id.textView5);
                        break;
                    case (2):
                        progressBar = (ProgressBar) findViewById(R.id.progressBar);
                        bg = (TextView) findViewById(R.id.textView);
                        name = (TextView) findViewById(R.id.textView5);
                        break;
                    case (3):
                        progressBar = (ProgressBar) findViewById(R.id.progressBar);
                        bg = (TextView) findViewById(R.id.textView);
                        name = (TextView) findViewById(R.id.textView5);
                        break;
                    case(4):
                        progressBar = (ProgressBar) findViewById(R.id.progressBar);
                        bg = (TextView) findViewById(R.id.textView);
                        name = (TextView) findViewById(R.id.textView5);
                        break;
                }
                UIDeviceList.add(new UIDevice(progressBar, bg, name, deviceIDStr));
            }
        }
    }

    public void startSvc(View view){
        Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
        ProgressBar progressBar=(ProgressBar) findViewById(R.id.progressBar);
        mHandler.postDelayed(updateProgress, 10000);
//        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(300);
        startService(mIntent);
        bindSvc();
    }

//    public void onServiceAvailable(){
//        serviceAvailable=true;
//        //Finish setting up our UIDevice groupings
//        ArrayList<AbstractDevice> cgms=mServer.getDevices();
//        for (AbstractDevice cgm:cgms) {
//            String deviceIDStr="device_"+cgm.getDeviceID();
//            for (UIDevice uidev : UIDeviceList) {
//                if (uidev.getDeviceID().equals(deviceIDStr)){
//                    uidev.setDevice(cgm);
//                }
//            }
//        }
//    }

    private Runnable updateProgress = new Runnable() {
        @Override
        public void run() {
            if (mBounded) {
                for (UIDevice uidev : UIDeviceList) {
                    uidev.update();
                }
            }
            mHandler.postDelayed(updateProgress,1000);
        }
    };

//    public void getData(View view){
//        if (mServer!=null) {
//            ArrayList<DeviceDownloadObject> data = mServer.getData();
//            TextView tv = (TextView) findViewById(R.id.textView);
//            for (DeviceDownloadObject datum : data) {
//                int lastIndex = datum.getEgvRecords().length - 1;
//                String msg="====================\n";
//                msg += "BG: " + datum.getEgvRecords()[lastIndex].getEgv() + "\n"
//                        + "Trend: " + datum.getEgvRecords()[lastIndex].getTrend().toString() + "\n"
//                        + "Date: " + datum.getEgvRecords()[lastIndex].getDate().toString() + "\n";
//                tv.append(msg);
//            }
//            tv.append("*********************\n");
//        }
//    }

    public void stopSvc(View view){
        Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
        if (mBounded) {
            unbindService(mConnection);
            mBounded=false;
        }
//        progressBar.setProgress(0);
//        progressBar.setVisibility(View.INVISIBLE);
        mHandler.removeCallbacks(updateProgress);
        stopService(mIntent);
    }

    public void bindSvc(){
        Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
        bindService(mIntent,mConnection,BIND_AUTO_CREATE);
    }

    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
            Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_LONG).show();
            mBounded = false;
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_LONG).show();
            mBounded = true;
            DeviceDownloadService.LocalBinder mLocalBinder = (DeviceDownloadService.LocalBinder)service;
            mServer = mLocalBinder.getServerInstance();
            ArrayList<AbstractDevice> cgms=mServer.getDevices();
            for (AbstractDevice cgm:cgms) {
                String deviceIDStr="device_"+cgm.getDeviceID();
                for (UIDevice uidev : UIDeviceList) {
                    if (uidev.getDeviceID().equals(deviceIDStr)){
                        uidev.setDevice(cgm);
                    }
                }
            }
        }
    };


//    public void toggleService(View view) {
//        Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
//        final Button button = (Button) findViewById(R.id.button);
//        if (isServiceRunning()) {
//            Log.d(TAG, "Stopping service");
//            stopService(mIntent);
//            button.setText("Start");
//        } else {
//            Log.d(TAG, "Starting service");
//            startService(mIntent);
//            button.setText("Stop");
//        }
//    }

//    private boolean isServiceRunning() {
//        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
//            if (DeviceDownloadService.class.getName().equals(service.service.getClassName())) {
//                return true;
//            }
//        }
//        return false;
//    }

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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBounded)
            unbindService(mConnection);
        unregisterReceiver(alarmReceiver);
    }

    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Received broadcast: "+intent.getAction());
            if (intent.getAction().equals("com.ktind.cgm.UI_READING_UPDATE")){
                Log.d(TAG,"Received a UI update");
                String bgReading=intent.getExtras().getString("bgReading","---");
                String deviceID=intent.getExtras().getString("deviceID","");
                Log.d(TAG,"Device: "+deviceID+" bgReading: "+bgReading);
                for (UIDevice uid:UIDeviceList){
                    if (uid.getDeviceID().equals(deviceID)){
                        uid.setBGDisplay(bgReading);
                    }
                }
            }
//            if (intent.getAction().equals("com.ktind.cgm.SERVICE_READY")){
//
////                onServiceAvailable();
//            }
        }
    }
}
