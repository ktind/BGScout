package com.ktind.cgm.bgscout;

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

    public AbstractDevice(String n, int deviceID, Context appContext, Handler mH){
        Log.i(TAG, "Creating "+getDeviceType()+" named " + n);
        setName(n);
        this.setDeviceID(deviceID);
        this.setAppContext(appContext);
        this.setHandler(mH);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        // TODO Copy/paste bad .... =(. Pull these values out to some central location
        String[] device_list={"device_1","device_2","device_3","device_4"};
        AbstractMonitor androidMonitor;
        AbstractMonitor mongoUpload;
//        AbstractMonitor mqttUpload=new MqttUploader(name,getAppContext());
//        monitors.add(mqttUpload);

        // FIXME there are more efficient ways to get this...
        for (String dev: device_list){
            if (sharedPref.getString(dev+"_name","").equals(getName())) {
                androidMonitor = new AndroidNotificationMonitor(getName(), deviceID, appContext);
                monitors.add(androidMonitor);
                // Defaulting to a remote client to be safe..
                if (Integer.valueOf(sharedPref.getString(dev + "_type", "1")) == 0) {
                    mongoUpload = new MongoUploadMonitor(getName(),appContext);
                    monitors.add(mongoUpload);
                }
            }
        }
        monitorProxy.setMonitors(monitors);
    }

    public void setHandler(Handler mH){
        this.mHandler=mH;
    }

    // TODO should probably be moved to a separate uploader object
    public float getUploaderBattery(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = appContext.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    abstract int getDeviceBattery() throws IOException;

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
        // FIXME - Not sure this is healthy....?
        MonitorProxy myProxy=new MonitorProxy(monitorProxy);
        try {
            myProxy.execute(getLastDownloadObject());
        } catch (NoDownloadException e){
            Log.e(TAG,"No start object to fire on");
        }
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
            Log.e(TAG, "No last start");
            throw new NoDownloadException();
        }
        return lastDownloadObject;
    }

    public void setLastDownloadObject(DeviceDownloadObject lastDownloadObject) {
        this.lastDownloadObject = lastDownloadObject;
    }

    @Override
    public void stop() {
        this.stopMonitors();
    }
}
