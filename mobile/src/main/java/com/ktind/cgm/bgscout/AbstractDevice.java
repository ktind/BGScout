package com.ktind.cgm.bgscout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
    protected GlucoseUnit unit=GlucoseUnit.MGDL;
    protected ArrayList<AbstractMonitor> monitors=new ArrayList<AbstractMonitor>();
    protected DeviceDownloadObject lastDownloadObject;
    protected Context appContext;
    protected CGMTransportAbstract cgmTransport;
    protected MonitorProxy monitorProxy=new MonitorProxy();
    protected boolean remote =false;
    protected Handler mHandler;
    protected String deviceType=null;
//    protected AsyncTask mTask;
    protected String deviceIDStr;
    // Set the default pollInterval to 5 minutes...
    protected long pollInterval=300000;

    public AbstractDevice(String n, int deviceID, Context appContext, Handler mH){
        Log.i(TAG, "Creating "+getDeviceType()+" named " + n);
        setName(n);
        this.setDeviceID(deviceID);
        this.setAppContext(appContext);
        this.setHandler(mH);
        this.deviceIDStr="device_"+String.valueOf(getDeviceID());

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        AbstractMonitor mon;
        // FIXME New monitor types must be added here, in the settings menu, and strings.xml in order to be used... Would be ideal if this framework could dynamically detect new monitor types and create them
        if (sharedPref.getBoolean(deviceIDStr+"_android_monitor",false)){
            mon=new AndroidNotificationMonitor(getName(),deviceID,getAppContext());
            monitors.add(mon);
        }
        if (sharedPref.getBoolean(deviceIDStr+"_mongo_upload",false)){
            mon=new MongoUploadMonitor(getName(),deviceID,getAppContext());
            monitors.add(mon);
        }
        if (sharedPref.getBoolean(deviceIDStr+"_push_upload",false)){
            mon=new MqttUploader(getName(),deviceID,getAppContext());
            monitors.add(mon);
        }
        if (sharedPref.getBoolean(deviceIDStr+"_nsapi_enable",false)){
            mon=new NightScoutUpload(getName(),deviceID,getAppContext());
            monitors.add(mon);
        }

        monitorProxy.setMonitors(monitors);
        Log.d(TAG,"Number of monitors created: "+monitors.size());
    }

    public void setHandler(Handler mH){
        this.mHandler=mH;
    }

    // TODO should probably be moved to a separate uploader object
    public float getUploaderBattery(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = appContext.registerReceiver(null, ifilter);
        assert batteryStatus != null;
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    abstract int getDeviceBattery() throws IOException, DeviceNotConnected;

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

    public GlucoseUnit getUnit() {
        return unit;
    }

    public void stopMonitors(){
        monitorProxy.stopMonitors();
    }

    public void setUnit(GlucoseUnit unit) {
        this.unit = unit;
    }

    @Override
    public void fireMonitors() {
        Log.d(TAG,"Firing monitors");
        for (AbstractMonitor monitor:monitors){
            try {
                monitor.doProcess(getLastDownloadObject());
            } catch (NoDownloadException e) {
                Log.w(TAG,"No last download for "+getName()+"("+getDeviceID()+")");
                e.printStackTrace();
            }
        }
//        // FIXME - Not sure this is healthy....?
//        MonitorProxy myProxy=new MonitorProxy(monitorProxy);
//        try {
//            mTask=myProxy.execute(getLastDownloadObject());
//        } catch (NoDownloadException e){
//            Log.e(TAG,"No process");
//        }
    }

    public boolean isConnected(){
        return cgmTransport.isOpen();
    }

    public void setAppContext(Context appContext) {
        this.appContext = appContext;
    }

//    public abstract float getUploaderBattery();

    public abstract void connect() throws DeviceNotConnected, IOException;

    public abstract void disconnect();

    public class NoDownloadException extends Throwable {
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

    public DeviceDownloadObject getLastDownloadObject() throws NoDownloadException {
        if (lastDownloadObject==null) {
            Log.e(TAG, "Last download object was not set");
            throw new NoDownloadException();
        }
        return lastDownloadObject;
    }

    public void setLastDownloadObject(DeviceDownloadObject lastDownloadObject) {
        this.lastDownloadObject = lastDownloadObject;
//        CGMBus.getInstance().post(lastDownloadObject);
    }
    public long getPollInterval() {
        return pollInterval;
    }


    public Date getNextReadingTime(){
        long lastReading;
        long msSinceLastReading;
        long multiplier;
        long timeForNextReading=getPollInterval();

        try {
            lastReading = getLastDownloadObject().getEgvRecords()[getLastDownloadObject().getEgvRecords().length - 1].getDate().getTime();
            msSinceLastReading = System.currentTimeMillis() - lastReading;
            multiplier = msSinceLastReading / getPollInterval();
            timeForNextReading = (System.currentTimeMillis() - msSinceLastReading) + (multiplier * getPollInterval());
        } catch (NoDownloadException e) {
            e.printStackTrace();
            Log.e(TAG,"Unable to determine next reading time because there hasn't been a previous reading");
        }
        if (timeForNextReading<0){
            Log.w(TAG,"Should not see this. Something is wrong with my math");
            timeForNextReading=getPollInterval();
        }
        return new Date(timeForNextReading);
    }

    public int getLastBG() throws NoDownloadException {
        int lastIndex= 0;
        lastIndex = getLastDownloadObject().getEgvRecords().length-1;
        return getLastDownloadObject().getEgvRecords()[lastIndex].getEgv();
    }

    public Trend getLastTrend() throws NoDownloadException {
        int lastIndex = 0;
        lastIndex = getLastDownloadObject().getEgvRecords().length - 1;
        return getLastDownloadObject().getEgvRecords()[lastIndex].getTrend();
    }

    public Date getLastDate() throws NoDownloadException {
        int lastIndex = 0;
        lastIndex = getLastDownloadObject().getEgvRecords().length - 1;
        return getLastDownloadObject().getEgvRecords()[lastIndex].getDate();
    }

    public EGVRecord getLastEGV() throws NoDownloadException {
        int lastIndex = 0;
        lastIndex = getLastDownloadObject().getEgvRecords().length - 1;
        return getLastDownloadObject().getEgvRecords()[lastIndex];
    }

    public void start() {
        Log.d(TAG,"Starting "+getName()+" (device_"+getDeviceID()+"/"+getDeviceType()+")");
    }


    protected void onDownload(){
        Intent uiIntent = new Intent("com.ktind.cgm.UI_READING_UPDATE");
        uiIntent.putExtra("deviceID",deviceIDStr);
        try {
            uiIntent.putExtra("bgReading",String.valueOf(getLastBG())+" "+getLastTrend().toString());
        } catch (NoDownloadException e) {
            uiIntent.putExtra("bgReading","---");
            e.printStackTrace();
        }
        Log.d(TAG,"Sending broadcast to UI: "+uiIntent.getExtras().getString("bgReading",""));
        appContext.sendBroadcast(uiIntent);
    }


        @Override
    public void stop() {
//        if (mTask!=null)
//            mTask.cancel(true);
        this.stopMonitors();
    }
}
