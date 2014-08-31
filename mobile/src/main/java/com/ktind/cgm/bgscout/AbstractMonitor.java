package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

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
//    protected int highThreshold=180;
//    protected int lowThreshold=60;
    protected int deviceID;
    protected String deviceIDStr;
    protected Context context;
    protected SharedPreferences sharedPref;
    protected long lastSuccessDate;
//    protected EGVLimits egvLimits;

    public AbstractMonitor(String n,int devID,Context context, String monitorName){
        this.setName(n);
        this.deviceID=devID;
        this.context =context;
        this.deviceIDStr="device_"+String.valueOf(deviceID);
        this.monitorType=monitorName;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        // Default to the last 2.5 hours as the "last successful download"
        this.lastSuccessDate=sharedPref.getLong(deviceIDStr+monitorType,new Date().getTime()-900000L);
        Log.d(TAG,"Setting lastSuccessDate to: "+new Date(lastSuccessDate));
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
            lastSuccessDate=sharedPref.getLong(deviceIDStr+monitorType,new Date().getTime()-900000L);
            Log.d(TAG, "Trimming data for monitor "+name+"/"+monitorType);
            final DownloadObject dl=new DownloadObject(d);
            dl.setEgvRecords(trimReadingsAfter(getlastSuccessDate(), d.getEgvArrayListRecords()));
            Log.d(TAG, "Processing monitor "+name+" for "+monitorType);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    doProcess(dl);
                }
            },"monitor_"+ monitorType + deviceIDStr).start();

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

    public void savelastSuccessDate(Date date){
        savelastSuccessDate(date.getTime());
    }

    @Override
    public void stop(){
        Log.i(TAG,"Stopping monitor "+monitorType+" for "+name);
    }

    @Override
    public void start(){
        Log.i(TAG,"Starting monitor "+monitorType+" for "+name);
    }

    public ArrayList<EGVRecord> trimReadingsAfter(Long afterDateLong, ArrayList<EGVRecord> egvRecords){
        ArrayList<EGVRecord> recs=new ArrayList<EGVRecord>(egvRecords);
        Log.d(TAG,"Size before trim: "+recs.size());
        Date afterDate=new Date(afterDateLong);
        for (Iterator<EGVRecord> iterator = recs.iterator(); iterator.hasNext(); ) {
            EGVRecord record = iterator.next();
            // trim anything after the date UNLESS that means we trim everything. Let's keep
            // the last record in there just in case. Need to find a better solution to this
            // the method doesn't reflect its purpose
            if (! record.getDate().after(afterDate) && recs.size()>1)
                iterator.remove();
        }
        Log.d(TAG,"Size after trim: "+recs.size()+" vs original "+egvRecords.size());
        return recs;
    }



//    public void setHighThreshold(int highThreshold) {
//        Log.v(TAG,"Setting high threshold to "+highThreshold);
//        this.highThreshold = highThreshold;
//    }
//
//    public void setLowThreshold(int lowThreshold) {
//        Log.v(TAG,"Setting low threshold to "+lowThreshold);
//        this.lowThreshold = lowThreshold;
//    }
}
