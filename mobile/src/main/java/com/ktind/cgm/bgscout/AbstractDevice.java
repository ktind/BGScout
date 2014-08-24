package com.ktind.cgm.bgscout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by klee24 on 8/7/14.
 */
public abstract class AbstractDevice implements DeviceInterface {
    private static final String TAG = AbstractDevice.class.getSimpleName();
    protected String name;
    protected int deviceID;
    protected GlucoseUnit unit=GlucoseUnit.NONE;
    protected ArrayList<AbstractMonitor> monitors=new ArrayList<AbstractMonitor>();
    protected DownloadObject lastDownloadObject;
    protected Context appContext;
    protected CGMTransportAbstract cgmTransport;
    protected boolean remote=false;
    protected Handler mHandler;
    protected String deviceType=null;
    protected String deviceIDStr;
    // Set the default pollInterval to 5 minutes...
    protected long pollInterval=303000;
    protected SharedPreferences sharedPref;
    protected DeviceStats stats=new DeviceStats();

    public AbstractDevice(String n, int deviceID, Context appContext, Handler mH) {
        Log.i(TAG, "Creating " + n);
        setName(n);
        this.setDeviceID(deviceID);
        this.setAppContext(appContext);
        this.setHandler(mH);
        this.deviceIDStr = "device_" + String.valueOf(getDeviceID());
        sharedPref=PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    public String getDeviceIDStr() {
        return deviceIDStr;
    }

    public void start(){
        BGScout.statsMgr.registerCollector(stats);
        Log.d(TAG,"Starting "+getName()+" (device_"+getDeviceID()+"/"+getDeviceType()+")");
        AbstractMonitor mon;
        if (sharedPref.getBoolean(deviceIDStr+"_android_monitor",false)){
            Log.i(TAG,"Adding a local android monitor");
            mon=new AndroidNotificationMonitor(getName(),deviceID,getAppContext());
            monitors.add(mon);
        }
        if (! isRemote()) {
            if (sharedPref.getBoolean(deviceIDStr + "_mongo_upload", false)) {
                Log.i(TAG, "Adding a mongo upload monitor");
                mon = new MongoUploadMonitor(getName(), deviceID, getAppContext());
                monitors.add(mon);
            }
            if (sharedPref.getBoolean(deviceIDStr + "_push_upload", false)) {
                Log.i(TAG, "Adding a push notification upload monitor");
                mon = new MqttUploader(getName(), deviceID, getAppContext());
                monitors.add(mon);
            }
            if (sharedPref.getBoolean(deviceIDStr + "_nsapi_enable", false)) {
                Log.i(TAG, "Adding a Nightscout upload monitor");
                mon = new NightScoutUpload(getName(), deviceID, getAppContext());
                monitors.add(mon);
            }
        } else {
            Log.i(TAG, "Ignoring monitors that do not allow remote devices");
        }
        Log.d(TAG,"Number of monitors created: "+monitors.size());
    }

    public void setHandler(Handler mH){
        this.mHandler=mH;
    }

    public float getUploaderBattery(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = appContext.registerReceiver(null, ifilter);
        assert batteryStatus != null;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    abstract public int getDeviceBattery() throws IOException, OperationNotSupportedException, NoDeviceFoundException, DeviceIOException;

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    public Context getAppContext() {
        return appContext;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public GlucoseUnit getUnit() throws DeviceException {
        return unit;
    }

    public void stopMonitors(){
        for (AbstractMonitor monitor:monitors){
            monitor.stop();
        }
    }

    public void setUnit(GlucoseUnit unit) {
        this.unit = unit;
    }

    @Override
    public void fireMonitors() {
        stats.startMonitorTimer();
        Log.d(TAG,"Firing monitors");
        for (AbstractMonitor monitor:monitors){
            try {
                monitor.process(getLastDownloadObject());
            } catch (DeviceException e) {
                Log.w(TAG,e.getMessage());
            }
        }
        stats.stopMonitorTimer();
    }

    public boolean isConnected(){
        return cgmTransport.isOpen();
    }

    public void setAppContext(Context appContext) {
        this.appContext = appContext;
    }

    public void connect() throws IOException, DeviceException {
        stats.addConnect();
    }

    public void disconnect(){
        stats.addDisconnect();
    }

    public String getDeviceType() {
        if (deviceType==null)
            return "unknown";
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public boolean isRemote() {
        return remote;
    }

    public DownloadObject getLastDownloadObject() throws NoDataException {
        if (lastDownloadObject==null) {
            throw new NoDataException("No previous download available");
        }
        return lastDownloadObject;
    }

    public void setLastDownloadObject(DownloadObject lastDownloadObject) {
        this.lastDownloadObject = lastDownloadObject;
    }
    public long getPollInterval() {
        return pollInterval;
    }


    public Date getNextReadingTime() {
        long lastReading;
        long msSinceLastReading;
        long multiplier;
        long timeForNextReading=getPollInterval();

        try {
            Date d = getLastDownloadObject().getLastReadingDate();
            lastReading = d.getTime();
            msSinceLastReading = System.currentTimeMillis() - lastReading;
            multiplier = msSinceLastReading / getPollInterval();
            timeForNextReading = (System.currentTimeMillis() - msSinceLastReading) + (multiplier * getPollInterval());
        } catch (NoDataException e) {
            timeForNextReading=new Date().getTime()+45000L;
        }
        if (timeForNextReading<0){
            Log.wtf(TAG,"Should not see this. Something is wrong with my math");
            timeForNextReading=getPollInterval();
        }
        return new Date(timeForNextReading);
    }

    public int getLastBG() throws NoDataException {
        int lastIndex= 0;
        lastIndex = getLastDownloadObject().getEgvRecords().length-1;
        if (lastIndex<0)
            throw new NoDataException("No previous download available");
        return getLastDownloadObject().getEgvRecords()[lastIndex].getEgv();
    }

    public Trend getLastTrend() throws NoDataException {
        int lastIndex = 0;
        lastIndex = getLastDownloadObject().getEgvRecords().length - 1;
        if (lastIndex<0)
            throw new NoDataException("No previous download available");
        return getLastDownloadObject().getEgvRecords()[lastIndex].getTrend();
    }

    protected void onDownload(){

//        Intent uiIntent = new Intent("com.ktind.cgm.UI_READING_UPDATE");
//        uiIntent.putExtra("deviceID",deviceIDStr);
//        DownloadObject downloadObject=null;
//        try {
//            downloadObject=getLastDownloadObject();
//        } catch (NoDataException e) {
//            downloadObject=new DownloadObject();
//            e.printStackTrace();
//        } finally {
//            if (downloadObject!=null) {
//                downloadObject.setDeviceID(deviceIDStr);
//                downloadObject.setDeviceName(getName());
////                downloadObject.buildMessage();
//                uiIntent.putExtra("download", downloadObject);
//            }
//        }
////        Log.d(TAG,"Sending broadcast to UI: "+uiIntent.getExtras().getString("download",""));
//        Log.d(TAG,"Name: "+downloadObject.getDeviceName());
//        appContext.sendBroadcast(uiIntent);
        sendToUI();
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(deviceIDStr + appContext.getText(R.string.last_g4_download), getLastDownloadObject().getLastReadingDate().getTime());
            editor.apply();
        } catch (NoDataException e) {
            e.printStackTrace();
        }
        fireMonitors();
    }

    public void sendToUI(){
        Intent uiIntent = new Intent("com.ktind.cgm.UI_READING_UPDATE");
        uiIntent.putExtra("deviceID",deviceIDStr);
        DownloadObject downloadObject=null;
        try {
            downloadObject=getLastDownloadObject();
        } catch (NoDataException e) {
            downloadObject=new DownloadObject();
            e.printStackTrace();
        } finally {
            if (downloadObject!=null) {
                downloadObject.setDeviceID(deviceIDStr);
                downloadObject.setDeviceName(getName());
//                downloadObject.buildMessage();
                uiIntent.putExtra("download", downloadObject);
            }
        }
//        Log.d(TAG,"Sending broadcast to UI: "+uiIntent.getExtras().getString("download",""));
        Log.d(TAG,"Name: "+downloadObject.getDeviceName());
        appContext.sendBroadcast(uiIntent);
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    @Override
    public void stop() {
        this.stopMonitors();
    }

    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.UIDO_QUERY)){
                Log.d(TAG,"Received a query from the main activity for the download object");
                sendToUI();
            }
        }
    }

}
