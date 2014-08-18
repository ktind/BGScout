package com.ktind.cgm.bgscout;

import android.content.Context;
import android.util.Log;

/**
 * Created by klee24 on 8/2/14.
 */
abstract public class AbstractMonitor implements MonitorInterface {
    private static final String TAG = DeviceDownloadService.class.getSimpleName();
    protected String name;
    protected boolean allowVirtual=false;
    protected String monitorType="generic";
    protected int highThreshold=180;
    protected int lowThreshold=60;
    int deviceID;
    String deviceIDStr;
    Context appContext;

    public AbstractMonitor(String n,int devID,Context context){
        this.setName(n);
        this.deviceID=devID;
        this.appContext=context;
        this.deviceIDStr="device_"+String.valueOf(deviceID);
    }

    public int getDeviceID() {
        return deviceID;
    }

    public void setDeviceID(int deviceID) {
        this.deviceID = deviceID;
    }

    public String getDeviceIDStr() {
        return deviceIDStr;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMonitorType() {
        return monitorType;
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    abstract protected void doProcess(DownloadObject d);

    public boolean isAllowVirtual() {
        return allowVirtual;
    }

    public void setAllowVirtual(boolean allowVirtual) {
        this.allowVirtual = allowVirtual;
    }

    @Override
    final public void process(DownloadObject d) {
        Log.d(TAG,"Monitor "+name+" has fired for "+monitorType);
        if (isAllowVirtual() || ! d.isRemoteDevice()){
            Log.d(TAG, "Processing monitor "+name+" for "+monitorType);
            this.doProcess(d);
        } else {
            Log.w(TAG, "Not processing monitor "+name+" for "+monitorType+" because device is classified as a remote device.");
        }
    }

    @Override
    public void stop(){
        Log.i(TAG,"Stopping monitor "+monitorType+" for "+name);
    }

    public void setHighThreshold(int highThreshold) {
        Log.v(TAG,"Setting high threshold to "+highThreshold);
        this.highThreshold = highThreshold;
    }

    public void setLowThreshold(int lowThreshold) {
        Log.v(TAG,"Setting low threshold to "+lowThreshold);
        this.lowThreshold = lowThreshold;
    }
}
