package com.ktind.cgm.bgscout;


import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 Copyright (c) 2014, Kevin Lee (klee24@gmail.com)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice, this
 list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */

public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private boolean mBounded=false;
    private DeviceDownloadService mServer;
    private Handler mHandler=new Handler();
    private ArrayList<UIDevice> UIDeviceList=new ArrayList<UIDevice>();

    private ArrayList<String> deviceList=new ArrayList<String>();
    private AlarmReceiver alarmReceiver;
    private boolean svcUp=false;
    private int direction=0;
    private DownloadObject ld;
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private String[] mDrawerMenuItems;
    private ActionBarDrawerToggle mDrawerToggle;
    private int numItemsInMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mDrawerLayout= (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList=(ListView) findViewById(R.id.left_drawer);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String[] devices={"device_1","device_2","device_3","device_4"};
        ArrayList<String> mDrawerMenuItemsArrList=new ArrayList<String>();
        for (String device:devices) {
            if (sharedPref.getBoolean(device+"_enable",false)){
                Log.d(TAG,device+" is enabled");
                mDrawerMenuItemsArrList.add(sharedPref.getString(device+"_name",device));
            }
        }
        mDrawerMenuItemsArrList.add("Start");
        mDrawerMenuItemsArrList.add("Stop");
        mDrawerMenuItemsArrList.add("Dump stats to log");
        mDrawerMenuItemsArrList.add("Settings");
        numItemsInMenu=mDrawerMenuItemsArrList.size();

        mDrawerMenuItems=mDrawerMenuItemsArrList.toArray(new String[mDrawerMenuItemsArrList.size()]);
//        mDrawerMenuItems=getResources().getStringArray(R.array.devices);
//        ActionBar actionBar = getActionBar();
//        actionBar.hide();
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,R.layout.drawer_list_item,mDrawerMenuItems));
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        UIDevice uiDevice = new UIDevice((ImageView) findViewById(R.id.main_display), (ImageView) findViewById(R.id.direction_image), (TextView) findViewById(R.id.reading_text), (TextView) findViewById(R.id.app_name), (ImageView) findViewById(R.id.uploader_battery_indicator), (TextView) findViewById(R.id.uploader_battery_label), (ImageView) findViewById(R.id.deviceBattery), (TextView) findViewById(R.id.device_battery_label));
        UIDeviceList.add(uiDevice);
        alarmReceiver = new AlarmReceiver();
        registerReceiver(alarmReceiver, new IntentFilter("com.ktind.cgm.UI_READING_UPDATE"));
        if (savedInstanceState!=null){
            ld=savedInstanceState.getParcelable("lastDownload");
            if (ld==null){
                Log.d(TAG,"Its null...");
            } else {
                for (UIDevice uid:UIDeviceList){
                    uid.update(ld);
                }
            }
//            UIDeviceList.get(0).update((DownloadObject) savedInstanceState.getSerializable("lastDownload"));
        }
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer icon to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description */
                R.string.drawer_close  /* "close drawer" description */
        ) {

            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
            }

            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (isServiceRunning(DeviceDownloadService.class)) {
            bindSvc();
        }

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    // FIXME breaks the rules - order here is must match the order the items were put into the string array(list)
    private void selectItem(int position){
        Log.d(TAG,"Position: "+position+ " number of items in menu: "+numItemsInMenu);
        if (position==(numItemsInMenu-1)) {
            Log.d(TAG,"Starting settings");
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        if (position==(numItemsInMenu-2)) {
            Log.d(TAG,"Dumping stats");
            BGScout.statsMgr.logStats();
        }
        if (position==(numItemsInMenu-3)) {
            Log.d(TAG,"Stopping service");
            Intent intent=new Intent(Constants.STOP_DOWNLOAD_SVC);
            getBaseContext().sendBroadcast(intent);
//            Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
//            bindSvc();
//            stopService(mIntent);
        }
        if (position==(numItemsInMenu-4)) {
            Log.d(TAG,"Starting service");
            Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
            startService(mIntent);
            bindSvc();
        }

    }


    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putParcelable("lastDownload", ld);
        super.onSaveInstanceState(savedInstanceState);
    }

    public void startSvc(View view){
        Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
        startService(mIntent);
        bindSvc();
        svcUp=true;
    }

    public void stopSvc(View view){
        Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
        if (mBounded) {
            unbindService(mConnection);
            mBounded=false;
        }
        stopService(mIntent);
        svcUp=false;
    }

    public void bindSvc(){
        Intent mIntent = new Intent(MainActivity.this, DeviceDownloadService.class);
        bindService(mIntent, mConnection, BIND_AUTO_CREATE);
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
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

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
        Log.d(TAG,"onDestory called");
        super.onDestroy();
        if (mBounded)
            unbindService(mConnection);
        unregisterReceiver(alarmReceiver);
    }

    public void dumpStats(View view){
        BGScout.statsMgr.logStats();
    }

    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.ktind.cgm.UI_READING_UPDATE")){
                Log.d(TAG,"Received a UI update");
                DownloadObject downloadObject=new DownloadObject();
//                downloadObject=downloadObject.buildFromJSON(intent.getExtras().getString("download", downloadObject.getJson().toString()));
                downloadObject=intent.getParcelableExtra("download");
                String bgReading= null;
                bgReading = String.valueOf(downloadObject.getLastReadingString());
                float uploaderBat=downloadObject.getUploaderBattery();
                int devBattery=downloadObject.getDeviceBattery();
                String devID=downloadObject.getDeviceID();
                String nm=downloadObject.getDeviceName();
                Log.i(TAG,"Uploader battery: "+uploaderBat);
                Log.i(TAG,"Device battery: "+devBattery);
                Log.i(TAG,"deviceID: "+devID);
                Log.i(TAG,"Reading: "+bgReading);
                Log.i(TAG,"Name: "+downloadObject.getDeviceName());
                ld=downloadObject;
                for (UIDevice uid:UIDeviceList){
                    uid.update(downloadObject);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent=new Intent();
        intent.setAction(Constants.UIDO_QUERY);
        getBaseContext().sendBroadcast(intent);
//        if (svcUp)
//            mHandler.post(updateProgress);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (svcUp) {
            bindSvc();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mHandler.removeCallbacks(updateProgress);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        mHandler.removeCallbacks(updateProgress);
        if (mBounded) {
            unbindService(mConnection);
            mBounded=false;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
//        menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    protected class UIDevice{
        protected TextView name;
        protected ImageView main_display;
        protected ImageView direction;
        protected TextView bg;
        protected DownloadObject lastDownload;
        protected ImageView uploaderBattery;
        protected TextView uploaderBatteryLabel;
        protected ImageView deviceBattery;
        protected TextView deviceBatteryLabel;
        int mainBGColor;
        int currentBGColor;

        public UIDevice(ImageView main, ImageView dir, TextView reading, TextView n, ImageView ubat, TextView ubatl, ImageView dbat, TextView dbatl){
            setMain_display(main);
//            main_display.setBackgroundColor(mainBGColor);
            setDirection(dir);
            setBg(reading);
            setName(n);
            setUploaderBattery(ubat);
            setDeviceBattery(dbat);
            setUploaderBatteryLabel(ubatl);
            setDeviceBatteryLabel(dbatl);
            deviceBattery.setImageResource(R.drawable.battery);
            uploaderBattery.setImageResource(R.drawable.battery);
        }

        public TextView getUploaderBatteryLabel() {
            return uploaderBatteryLabel;
        }

        public void setUploaderBatteryLabel(TextView uploaderBatteryLabel) {
            this.uploaderBatteryLabel = uploaderBatteryLabel;
        }

        public TextView getDeviceBatteryLabel() {
            return deviceBatteryLabel;
        }

        public void setDeviceBatteryLabel(TextView deviceBatteryLabel) {
            this.deviceBatteryLabel = deviceBatteryLabel;
        }

        public ImageView getUploaderBattery() {
            return uploaderBattery;
        }

        public void setUploaderBattery(ImageView uploaderBattery) {
            this.uploaderBattery = uploaderBattery;
        }

        public ImageView getDeviceBattery() {
            return deviceBattery;
        }

        public void setDeviceBattery(ImageView deviceBattery) {
            this.deviceBattery = deviceBattery;
        }

        public void update(DownloadObject dl){
            lastDownload=dl;
            name.setText(dl.getDeviceName());
            direction.setImageResource(R.drawable.trendarrows);

            try {
                direction.setImageLevel(dl.getLastTrend().getVal());
                int r=dl.getLastReading();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                int highThreshold=Integer.parseInt(sharedPref.getString(dl.getDeviceID() + "_high_threshold", "180"));
                int lowThreshold=Integer.parseInt(sharedPref.getString(dl.getDeviceID() + "_low_threshold", "60"));
//                int newColor=Color.WHITE;
                if (r>highThreshold) {
                    currentBGColor=Color.rgb(255, 199, 0);
                }else if (r<lowThreshold){
                    currentBGColor=Color.RED;
                } else {
                    currentBGColor=Color.rgb(0,170,0);
                }
                mainBGColor=currentBGColor;
                main_display.setBackgroundColor(mainBGColor);
                bg.setText(String.valueOf(r));

                int dbat=dl.getDeviceBattery();
                deviceBatteryLabel.setText(String.valueOf(dbat));

                deviceBattery.setImageLevel(dbat);

                float ubat=dl.getUploaderBattery();
                uploaderBatteryLabel.setText(String.valueOf((int) ubat));
                uploaderBattery.setImageLevel((int) ubat);


            } catch (NoDataException e) {
                Log.d(TAG,"No data in previous download",e);
            }
        }

        public TextView getName() {
            return name;
        }

        public void setName(TextView name) {
            this.name = name;
        }

        public ImageView getMain_display() {
            return main_display;
        }

        public void setMain_display(ImageView main_display) {
            this.main_display = main_display;
        }

        public ImageView getDirection() {
            return direction;
        }

        public void setDirection(ImageView direction) {
            this.direction = direction;
        }

        public TextView getBg() {
            return bg;
        }

        public void setBg(TextView bg) {
            this.bg = bg;
        }
    }
}
