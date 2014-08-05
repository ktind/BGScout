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
import java.util.Date;

/**
 * Created by klee24 on 8/2/14.
 */
abstract public class AbstractCGMDevice implements CGMDeviceInterface {
    private static final String TAG = DeviceDownloadService.class.getSimpleName();
    protected String name;
    protected int deviceID;
//    private Date date;
    protected GlucoseUnit unit =GlucoseUnit.MGDL;
    protected ArrayList<AbstractMonitor> monitors=new ArrayList<AbstractMonitor>();
    protected DeviceDownloadObject lastDownloadObject;
    protected Context appContext;
    protected CGMTransportAbstract cgmTransport;
    protected MonitorProxy monitorProxy=new MonitorProxy();
    protected boolean virtual =false;
    protected long nextFire=45000L;
    protected Handler mHandler;
    protected int pollInterval=302000;

    public boolean isVirtual() {
        return virtual;
    }

    public AbstractCGMDevice(String n,int deviceID,Context appContext,Handler mH){
        Log.i(TAG, "Creating CGM " + n);
        setName(n);
        this.setDeviceID(deviceID);
        this.setAppContext(appContext);
        this.setHandler(mH);

//        AbstractMonitor anm=new AndroidNotificationMonitor(getName(),deviceID,appContext);
//        AbstractMonitor mongo=new MongoUploadMonitor(getName());
//        monitors.add(anm);
//        monitors.add(mongo);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        // TODO Copy/paste bad .... =(. Pull these values out to some central location
        String[] device_list={"device_1","device_2","device_3","device_4"};
        AbstractMonitor androidMonitor;
        AbstractMonitor mongoUpload;
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

    public float getUploaderBattery(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = appContext.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        return level / (float) scale;
    }

    abstract int getCGMBattery() throws IOException;

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    public Context getAppContext() {
        return appContext;
    }

    final public DeviceDownloadObject download(){
        lastDownloadObject=this.doDownload();
//        lastDownloadObject.setLastDownloadDate(new Date());
        return lastDownloadObject;
    }

    abstract protected DeviceDownloadObject doDownload();

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
        myProxy.execute(lastDownloadObject);
    }



    public boolean isConnected(){
        return cgmTransport.isOpen();
    }

    public void setAppContext(Context appContext) {
        this.appContext = appContext;
    }

//    public abstract float getUploaderBattery();

    public abstract void connect() throws DeviceNotConnected;

    public abstract void disconnect();

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        Log.d(TAG,"Setting poll interval to: "+pollInterval);
        this.pollInterval = pollInterval;
    }

    public long nextFire(){
        return nextFire(getPollInterval());
    }

    public long nextFire(long millis){
        // FIXME shouldn't assume this data is set. Need to wrap this in a method.
        if (lastDownloadObject!=null && lastDownloadObject.getEgvRecords()!=null && lastDownloadObject.getEgvRecords().length>0){
            long diff=(millis-(new Date().getTime() - lastDownloadObject.getEgvRecords()[lastDownloadObject.getEgvRecords().length-1].getDate().getTime()));
            Log.d(TAG,"nextFire calculated to be: "+diff+" for "+getName()+" using a poll interval of "+millis);
            if (diff<0) {
                Log.d(TAG,"nextFire returning 45 seconds because diff was negative");
                return 45000;
            }
            return diff;
        } else {
            Log.d(TAG,"nextFire returning 45 seconds because there wasn't a lastdownloadobject set");
            return 450000;
        }
    }

//    public abstract G4EGVRecord[] getReadings();
}
