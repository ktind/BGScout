package com.ktind.cgm.bgscout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

/**
 *
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
public abstract class AbstractDevice implements DeviceInterface {
    private static final String TAG = AbstractDevice.class.getSimpleName();
    protected String name;
    protected int deviceID;
    protected GlucoseUnit unit=GlucoseUnit.NONE;
    protected ArrayList<AbstractMonitor> monitors;
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
    protected boolean started=false;

    public AbstractDevice(String n, int deviceID, Context appContext, Handler mH) {
        Log.i(TAG, "Creating " + n);
        setName(n);
        this.setDeviceID(deviceID);
        this.setAppContext(appContext);
        this.setHandler(mH);
        this.deviceIDStr = "device_" + String.valueOf(getDeviceID());
        sharedPref=PreferenceManager.getDefaultSharedPreferences(appContext);
        monitors=new ArrayList<AbstractMonitor>();
    }

    public String getDeviceIDStr() {
        return deviceIDStr;
    }

    public void start(){
        BGScout.statsMgr.registerCollector(stats);
        Log.d(TAG,"Starting "+getName()+" (device_"+getDeviceID()+"/"+getDeviceType()+")");
        AbstractMonitor mon;
        monitors=new ArrayList<AbstractMonitor>();

        // FIXME - Mandatory monitor
        mon=new SQLiteMonitor(getName(),deviceID,getAppContext());
        monitors.add(mon);

        if (sharedPref.getBoolean(deviceIDStr + "_android_monitor", false)) {
            Log.i(TAG, "Adding a local android monitor");
            mon = new AndroidNotificationMonitor(getName(), deviceID, getAppContext());
            monitors.add(mon);
        }
        if (!isRemote()) {
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
        Log.d(TAG, "Number of monitors created: " + monitors.size());
        started=true;
    }

//    public void mainloop(){
//        while(started){
//
//        }
//    }
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

    public GlucoseUnit getUnit() {
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
        sendToUI();
        try {
            if (Looper.getMainLooper().getThread()==Thread.currentThread())
                Log.d(TAG,"ON THE MAIN Thread!!!");
            else
                Log.d(TAG, "Not on the MAIN Thread ("+Thread.currentThread().getName()+"/"+Thread.currentThread().getState()+")");
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong(deviceIDStr + appContext.getText(R.string.last_g4_download), getLastDownloadObject().getLastReadingDate().getTime());
            editor.apply();
        } catch (NoDataException e) {
            Log.i(TAG,"No data on download");
//            e.printStackTrace();
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
        started=false;
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
