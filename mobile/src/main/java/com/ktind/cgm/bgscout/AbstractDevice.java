package com.ktind.cgm.bgscout;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;

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
    protected Context context;
    protected CGMTransportAbstract cgmTransport;
    protected boolean remote=false;
//    protected Handler mHandler;
    protected String deviceType=null;
    protected String deviceIDStr;
    // Set the default pollInterval to 5 minutes...
    protected long pollInterval=303000;
    protected SharedPreferences sharedPref;
    protected DeviceStats stats=new DeviceStats();
    protected boolean started=false;
    protected AlarmReceiver uiQuery;
    protected State state;
    protected String driver;

    protected String phoneNum;

    public AbstractDevice(String name, int deviceID, Context context, String driver) {
        Log.i(TAG, "Creating " + name+"/"+driver);
        this.name=name;
        this.driver=driver;
        this.setDeviceID(deviceID);
        this.setContext(context);
        this.deviceIDStr = "device_" + String.valueOf(getDeviceID());
        sharedPref=PreferenceManager.getDefaultSharedPreferences(context);
        state=State.STOPPED;
        String contactDataUri=sharedPref.getString(deviceIDStr+"_contact_data_uri",Uri.EMPTY.toString());

        if (!contactDataUri.equals(Uri.EMPTY.toString()))
            phoneNum=getPhone(contactDataUri);
    }

    public String getContactNum() {
        return phoneNum;
    }

    public String getDeviceIDStr() {
        return deviceIDStr;
    }

    public void start(){
        if (state==State.STARTED || state==State.STARTING){
            Log.w(TAG, getName()+"/"+getDeviceType()+" has already been started");
            return;
        }
        state=State.STARTING;
        BGScout.statsMgr.registerCollector(stats);
        Log.d(TAG,"Starting "+getName()+" (device_"+getDeviceID()+"/"+getDeviceType()+")");
        AbstractMonitor mon;
        monitors=new ArrayList<AbstractMonitor>();

        monitors.add(new SQLiteMonitor(getName(),deviceID, getContext()));

        if (sharedPref.getBoolean(deviceIDStr + "_pebble_monitor", false)) {
            Log.i(TAG, "Adding a Pebble monitor");
            monitors.add(new PebbleMonitor(getName(),deviceID, getContext()));
        }

//        if (sharedPref.getBoolean(deviceIDStr + "_android_monitor", false)) {
//            Log.i(TAG, "Adding a local android monitor");
//            mon = new AndroidNotificationMonitor(getName(), deviceID, getContext());
//            if (phoneNum!=null) {
//                ((AndroidNotificationMonitor) mon).setPhoneNum(phoneNum);
//            }
//            monitors.add(mon);
//        }
        if (!isRemote()) {
            if (sharedPref.getBoolean(deviceIDStr + "_mongo_upload", false)) {
                Log.i(TAG, "Adding a mongo upload monitor");
                mon = new MongoUploadMonitor(getName(), deviceID, getContext());
                monitors.add(mon);
            }
            if (sharedPref.getBoolean(deviceIDStr + "_push_upload", false)) {
                Log.i(TAG, "Adding a push notification upload monitor");
                mon = new MqttUploader(getName(), deviceID, getContext());
                monitors.add(mon);
            }
            if (sharedPref.getBoolean(deviceIDStr + "_nsapi_enable", false)) {
                Log.i(TAG, "Adding a Nightscout upload monitor");
                mon = new NightScoutUpload(getName(), deviceID, getContext());
                monitors.add(mon);
            }
        } else {
            Log.i(TAG, "Ignoring monitors that do not allow remote devices");
        }
//        mon=new WearMonitor(getName(),deviceID,getContext());
//        monitors.add(mon);

        // TODO Remove option since it longer does anything
        Log.i(TAG, "Adding a local android monitor");
        mon = new AndroidNotificationMonitor(getName(), deviceID, getContext());
        if (phoneNum!=null) {
            ((AndroidNotificationMonitor) mon).setPhoneNum(phoneNum);
        }
        monitors.add(mon);
        Log.d(TAG, "Number of monitors created: " + monitors.size());
        started=true;
        IntentFilter intentFilter=new IntentFilter(Constants.UIDO_QUERY);
        uiQuery=new AlarmReceiver();
        context.registerReceiver(uiQuery,intentFilter);
    }

    public float getUploaderBattery(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
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

    public Context getContext() {
        return context;
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
        if (monitors==null) {
            Log.w(TAG, "monitors is null. A stop was previously issued. How did I get here?");
            return;
        }
        Log.d(TAG,"stopMonitors called");
        for (AbstractMonitor monitor:monitors){
            monitor.stop();
        }
        monitors=null;
    }

    public void setUnit(GlucoseUnit unit) {
        this.unit = unit;
    }

    @Override
    public void fireMonitors(DownloadObject dl) {
        stats.startMonitorTimer();
        Log.d(TAG,"Firing monitors");
        if (monitors==null)
            return;
        for (AbstractMonitor monitor:monitors)
            monitor.process(dl);
        stats.stopMonitorTimer();
    }

    public boolean isConnected(){
        return cgmTransport.isOpen();
    }

    public void setContext(Context appContext) {
        this.context = appContext;
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
        lastIndex = getLastDownloadObject().getEgvArrayListRecords().size()-1;
        if (lastIndex<0)
            throw new NoDataException("No previous download available");
        return getLastDownloadObject().getEgvArrayListRecords().get(lastIndex).getEgv();
    }

    public Trend getLastTrend() throws NoDataException {
        int lastIndex = 0;
        lastIndex = getLastDownloadObject().getEgvArrayListRecords().size() - 1;
        if (lastIndex<0)
            throw new NoDataException("No previous download available");
        return getLastDownloadObject().getEgvArrayListRecords().get(lastIndex).getTrend();
    }

    protected void onDownload(DownloadObject dl){
        sendToUI();
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
//        SharedPreferences.Editor editor = sharedPref.edit();
//        editor.putLong(deviceIDStr +"_last_"+driver, dl.getLastReadingDate().getTime());
//        editor.apply();
        fireMonitors(dl);
        GoogleAnalytics.getInstance(context.getApplicationContext()).dispatchLocalHits();
    }

    public void sendToUI(){
        Intent uiIntent = new Intent(Constants.UI_UPDATE);
        uiIntent.putExtra("deviceID",deviceIDStr);
        DownloadObject downloadObject=null;
        try {
            downloadObject=getLastDownloadObject();
            Log.d(TAG,"Name: "+downloadObject.getDeviceName());
        } catch (NoDataException e) {
            downloadObject=new DownloadObject();
            Log.e(TAG,"Sending empty DownloadObject",e);
        } finally {
            if (downloadObject!=null) {
                downloadObject.setDeviceID(deviceIDStr);
                downloadObject.setDeviceName(getName());
                uiIntent.putExtra("download", downloadObject);
            }
        }
        context.sendBroadcast(uiIntent);
    }

    public void setRemote(boolean remote) {
        this.remote = remote;
    }

    @Override
    public void stop() {
        if (state==State.STOPPED || state==State.STOPPING) {
            Log.w(TAG,getName()+"/"+getDeviceType()+" has already been stopped or is being stopped");
            return;
        }
        state=State.STOPPING;
        this.stopMonitors();
        if (context !=null && uiQuery!=null)
            context.unregisterReceiver(uiQuery);
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

    private String getPhone(String uriString){
        return getPhone(Uri.parse(uriString));
    }

    private String getPhone(Uri dataUri){
        String id=dataUri.getLastPathSegment();
        Log.d(TAG,"id="+id);
        Log.d(TAG,"URI="+dataUri);
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null, ContactsContract.Data._ID + " = ?", new String[]{id}, null);
        int numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        Log.d(TAG, "cursor.getCount(): " + cursor.getCount());
        String phoneNum=null;
        if (cursor.moveToFirst()){
            phoneNum=cursor.getString(numIdx);
        }
        cursor.close();
        return phoneNum;
    }

}
