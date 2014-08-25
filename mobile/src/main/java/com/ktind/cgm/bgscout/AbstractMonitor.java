package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

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
abstract public class AbstractMonitor implements MonitorInterface {
    private static final String TAG = AbstractMonitor.class.getSimpleName();
    protected String name;
    protected boolean allowVirtual=false;
    protected String monitorType="generic";
    protected int highThreshold=180;
    protected int lowThreshold=60;
    protected int deviceID;
    protected String deviceIDStr;
    protected Context appContext;
    protected SharedPreferences sharedPref;
    protected long lastSuccessDate;

    public AbstractMonitor(String n,int devID,Context context, String monitorName){
        this.setName(n);
        this.deviceID=devID;
        this.appContext=context;
        this.deviceIDStr="device_"+String.valueOf(deviceID);
        this.monitorType=monitorName;
        this.sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        this.setLowThreshold(Integer.valueOf(sharedPref.getString(deviceIDStr + "_low_threshold", "60")));
        this.setHighThreshold(Integer.valueOf(sharedPref.getString(deviceIDStr + "_high_threshold", "180")));
        // Default to the last 2.5 hours as the "last successful download"
        this.lastSuccessDate=sharedPref.getLong(deviceIDStr+monitorType,new Date().getTime()-900000L);
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
            d.trimReadingsAfter(getlastSuccessDate());
            this.doProcess(d);
        } else {
            Log.w(TAG, "Not processing monitor "+name+" for "+monitorType+" because device is classified as a remote device.");
        }
    }

    public long getlastSuccessDate(){
        return lastSuccessDate;
    }

    // It is up to each implementation to save the last successful date.
    public void savelastSuccessDate(long date){
        SharedPreferences.Editor editor=sharedPref.edit();
        editor.putLong(deviceIDStr+monitorType,date);
        editor.apply();
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
