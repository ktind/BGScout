package com.ktind.cgm.bgscout;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.acra.*;
import org.acra.annotation.*;
import org.acra.sender.HttpSender;

import java.util.ArrayList;


@ReportsCrashes(formKey = "",
        mode = ReportingInteractionMode.TOAST,
        forceCloseDialogAfterToast = true,
        resToastText = R.string.crash_toast_text,
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://nightscout.iriscouch.com/acra-undefined/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "",
        formUriBasicAuthPassword = ""
)
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        UIDevice uiDevice=new UIDevice((ImageView) findViewById(R.id.main_display),(ImageView) findViewById(R.id.direction_image), (TextView) findViewById(R.id.reading_text), (TextView) findViewById(R.id.name), (ImageView) findViewById(R.id.uploader_battery_indicator), (TextView) findViewById(R.id.uploader_battery_label) ,(ImageView) findViewById(R.id.deviceBattery), (TextView) findViewById(R.id.device_battery_label));
        UIDeviceList.add(uiDevice);
        alarmReceiver=new AlarmReceiver();
        registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.UI_READING_UPDATE"));
//        registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.SERVICE_READY"));
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
        bindService(mIntent,mConnection,BIND_AUTO_CREATE);
    }

    ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceDisconnected(ComponentName name) {
//            Toast.makeText(MainActivity.this, "Service is disconnected", Toast.LENGTH_LONG).show();
            mBounded = false;
            mServer = null;
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
//            Toast.makeText(MainActivity.this, "Service is connected", Toast.LENGTH_LONG).show();
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

    public void dumpStats(View view){
        BGScout.statsMgr.logStats();
    }

    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG,"Received broadcast: "+intent.getAction());
            if (intent.getAction().equals("com.ktind.cgm.UI_READING_UPDATE")){
                Log.d(TAG,"Received a UI update");
                DownloadObject downloadObject=new DownloadObject();
                downloadObject=downloadObject.buildFromJSON(intent.getExtras().getString("download", downloadObject.getJson().toString()));
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
                for (UIDevice uid:UIDeviceList){
                    uid.update(downloadObject);
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
            try {
                switch (dl.getLastRecord().getTrend()){
                    case DOUBLEUP:
                        direction.setImageResource(R.drawable.doubleup);
                        break;
                    case SINGLEUP:
                        direction.setImageResource(R.drawable.up);
                        break;
                    case FORTYFIVEUP:
                        direction.setImageResource(R.drawable.fortyfiveup);
                        break;
                    case FLAT:
                        direction.setImageResource(R.drawable.flat);
                        break;
                    case FORTYFIVEDOWN:
                        direction.setImageResource(R.drawable.fortyfivedown);
                        break;
                    case SINGLEDOWN:
                        direction.setImageResource(R.drawable.down);
                        break;
                    case DOUBLEDOWN:
                        direction.setImageResource(R.drawable.doubledown);
                        break;
                    default:
                        direction.setImageResource(R.drawable.dash);
                        break;
                }
                int r=dl.getLastReading();
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                int highThreshold=Integer.parseInt(sharedPref.getString(dl.getDeviceID() + "_high_threshold", "180"));
                int lowThreshold=Integer.parseInt(sharedPref.getString(dl.getDeviceID() + "_low_threshold", "60"));
//                int newColor=Color.WHITE;
                if (r>highThreshold) {
                    currentBGColor=Color.RED;
                }else if (r<lowThreshold){
                    currentBGColor=Color.rgb(255, 199, 0);
                } else {
                    currentBGColor=Color.rgb(0,170,0);
                }
                mainBGColor=currentBGColor;
                main_display.setBackgroundColor(mainBGColor);
                bg.setText(String.valueOf(r));

                int dbat=dl.getDeviceBattery();
                deviceBatteryLabel.setText(String.valueOf(dbat));
                if (dbat > 75){
                    deviceBattery.setImageResource(R.drawable.batteryfullhorizontal);
                }else if (dbat <=75 && dbat > 50){
                    deviceBattery.setImageResource(R.drawable.battery75horizontal);
                }else if (dbat <=50 && dbat > 25){
                    deviceBattery.setImageResource(R.drawable.battery50horizontal);
                }else if (dbat <=25 && dbat > 15){
                    deviceBattery.setImageResource(R.drawable.battery25horizontal);
                }else if (dbat<=15 && dbat > 8) {
                    deviceBattery.setImageResource(R.drawable.batterylowhorizontal);
                } else if (dbat<8){
                    deviceBattery.setImageResource(R.drawable.batterycriticalhorizontal);
                }

                float ubat=dl.getUploaderBattery();
                uploaderBatteryLabel.setText(String.valueOf((int) ubat));
                if (ubat > 75){
                    uploaderBattery.setImageResource(R.drawable.batteryfullvertical);
                }else if (ubat <=75 && ubat > 50){
                    uploaderBattery.setImageResource(R.drawable.battery75vertical);
                }else if (ubat <=50 && ubat > 25){
                    uploaderBattery.setImageResource(R.drawable.battery50vertical);
                }else if (ubat <=25 && ubat > 15){
                    uploaderBattery.setImageResource(R.drawable.battery25vertical);
                }else if (ubat<=15 && ubat > 8) {
                    uploaderBattery.setImageResource(R.drawable.batterylowvertical);
                } else if (ubat<8){
                    uploaderBattery.setImageResource(R.drawable.batterycriticalvertical);
                }


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
