package com.ktind.cgm.bgscout;

import android.util.Log;

/**
 * Created by klee24 on 8/2/14.
 */
abstract public class AbstractMonitor implements MonitorInterface {
    private static final String TAG = DeviceDownloadService.class.getSimpleName();
    protected String name;
    //FIXME find a better way to manage this?
    protected boolean allowVirtual=false;
    protected String monitorType="generic";
    protected int highThreshold=180;
    protected int lowThreshold=60;

    public AbstractMonitor(String n){
        this.name=n;
    }

    public String getMonitorType() {
        return monitorType;
    }

    public void setMonitorType(String monitorType) {
        this.monitorType = monitorType;
    }

    abstract protected void doProcess(DeviceDownloadObject d);

    public boolean isAllowVirtual() {
        return allowVirtual;
    }

    public void setAllowVirtual(boolean allowVirtual) {
        this.allowVirtual = allowVirtual;
    }

    @Override
    final public void process(DeviceDownloadObject d) {
        Log.d(TAG,"Monitor "+name+" has fired for "+monitorType);
        if (isAllowVirtual() || ! d.getDevice().isVirtual()){
            Log.d(TAG, "Processing monitor "+name+" for "+monitorType);
            this.doProcess(d);
        } else {
            Log.d(TAG, "Not processing monitor "+name+" for "+monitorType);
        }
    }

    @Override
    public void stop(){
        Log.i(TAG,"Stopping monitor "+monitorType+" for "+name);
    }

    public void setHighThreshold(int highThreshold) {
        this.highThreshold = highThreshold;
    }

    public void setLowThreshold(int lowThreshold) {
        this.lowThreshold = lowThreshold;
    }
}
