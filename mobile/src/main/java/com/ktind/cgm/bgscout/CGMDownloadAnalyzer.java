/*
 * Copyright (c) 2014. , Kevin Lee (klee24@gmail.com)
 * All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification,
 *  are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, this
 *  list of conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by klee24 on 8/28/14.
 */
public abstract class CGMDownloadAnalyzer extends AbstractDownloadAnalyzer {
    protected final int UPLOADERBATTERYWARN =30;
    protected final int UPLOADERBATTERYCRITICAL =20;
    protected final int DEVICEBATTERYWARN =30;
    protected final int DEVICEBATTERYCRITICAL =20;
    protected final int MAXRECORDAGE=310000;
    protected EGVLimits egvLimits=new EGVLimits();
//    protected EGVThresholdsEnum conditions=EGVThresholdsEnum.INRANGE;
    

    CGMDownloadAnalyzer(DownloadObject dl,Context context) {
        super(dl);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        EGVThresholds warnThreshold=new EGVThresholds();
        EGVThresholds criticalThreshold=new EGVThresholds();
        Resources res=context.getResources();

        warnThreshold.setLowThreshold(Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_low_threshold", String.valueOf(res.getInteger(R.integer.pref_default_device_low)))));
        warnThreshold.setHighThreshold(Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_high_threshold", String.valueOf(res.getInteger(R.integer.pref_default_device_high)))));

        criticalThreshold.setLowThreshold(Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_critical_low_threshold", String.valueOf(res.getInteger(R.integer.pref_default_critical_device_low)))));
        criticalThreshold.setHighThreshold(Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_critical_high_threshold", String.valueOf(res.getInteger(R.integer.pref_default_critical_device_high)))));

        egvLimits.setWarnThreshold(warnThreshold);
        egvLimits.setCriticalThreshold(criticalThreshold);
        Log.d(TAG,"Critical low threshold: "+egvLimits.getCriticalLow());
        Log.d(TAG,"Warn low threshold: "+egvLimits.getWarnLow());
        Log.d(TAG,"Warn high threshold: "+egvLimits.getWarnHigh());
        Log.d(TAG,"Critical high threshold: "+egvLimits.getCriticalHigh());
    }

    public AnalyzedDownload analyze() {
        try {
            super.analyze();
            checkDownloadStatus();
            checkUploaderBattery();
            checkCGMBattery();
            checkRecordAge();
            checkThresholdholds();
            checkLastRecordTime();
        } catch (NoDataException e) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.WARN,"Download did not contain any data"),Conditions.NODATA);
//            e.printStackTrace();
        }
        return this.downloadObject;
    }

    protected void checkUploaderBattery(){
        if (downloadObject.getUploaderBattery() < UPLOADERBATTERYWARN) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.WARN, "Uploader battery is low: "+(int) downloadObject.getUploaderBattery()),Conditions.UPLOADERLOW);
        } else if (downloadObject.getUploaderBattery() < UPLOADERBATTERYCRITICAL) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "Uploader battery is critically low: "+(int) downloadObject.getUploaderBattery()),Conditions.UPLOADERCRITICALLOW);
        }
    }

    protected void checkCGMBattery(){
        if (downloadObject.getStatus()==DownloadStatus.SUCCESS) {
            if (downloadObject.getDeviceBattery() < DEVICEBATTERYWARN) {
                downloadObject.addMessage(new AlertMessage(AlertLevels.WARN, "CGM battery is low: "+downloadObject.getDeviceBattery()),Conditions.DEVICELOW);
            } else if (downloadObject.getDeviceBattery() < DEVICEBATTERYCRITICAL) {
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "CGM battery is critically low"+downloadObject.getDeviceBattery()),Conditions.DEVICECRITICALLOW);
            }
        }
    }

    protected void checkRecordAge() throws NoDataException {
        Long recordAge=new Date().getTime() - downloadObject.getLastRecordReadingDate().getTime();
        if (recordAge > MAXRECORDAGE)
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Last record is greater than "+((recordAge/1000)/60)+" minutes old"),Conditions.STALEDATA);
        Long downloadAge=new Date().getTime() - downloadObject.getDownloadDate().getTime();
        if (downloadAge > MAXRECORDAGE)
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Have not heard from remote CGM for more than "+((downloadAge/1000)/60)+" minutes"),Conditions.STALEDATA);
    }

    protected void checkDownloadStatus(){
        DownloadStatus status=downloadObject.getStatus();
        switch (status){
            case DEVICENOTFOUND:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"No CGM device found"),Conditions.DEVICEDISCONNECTED);
                break;
            case IOERROR:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unable to read or write to the CGM"),Conditions.DOWNLOADFAILED);
                break;
            case NODATA:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"No data in download"),Conditions.NODATA);
                break;
            case APPLICATIONERROR:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unknown application error"),Conditions.UNKNOWN);
                break;
            case UNKNOWN:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unknown error while trying to retrieve data from CGM"),Conditions.UNKNOWN);
                break;
            case REMOTEDISCONNECTED:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unable to connect to remote devices"),Conditions.REMOTEDISCONNECTED);
            default:
                break;
        }
    }

    protected void checkThresholdholds() throws NoDataException {
        int egv=downloadObject.getLastReading();
        Trend trend=downloadObject.getLastTrend();
        GlucoseUnit unit=downloadObject.getUnit();
        AlertLevels alertLevel=AlertLevels.INFO;
        Conditions condition= Conditions.INRANGE;
        if (egv > egvLimits.getCriticalHigh()) {
            condition=Conditions.CRITICALHIGH;
            alertLevel = AlertLevels.CRITICAL;
        }else if (egv < egvLimits.getCriticalLow()) {
            condition=Conditions.CRITICALLOW;
            alertLevel = AlertLevels.CRITICAL;
        }else if (egv > egvLimits.getWarnHigh()) {
            condition=Conditions.WARNHIGH;
            alertLevel = AlertLevels.WARN;
        }else if (egv < egvLimits.getWarnLow()) {
            condition=Conditions.WARNLOW;
            alertLevel = AlertLevels.WARN;
        }
        downloadObject.addMessage(new AlertMessage(alertLevel,egv+" "+unit+" "+trend),condition);
    }

    protected void checkLastRecordTime() throws NoDataException {
//        String msg=new SimpleDateFormat("HH:mm:ss MM/dd").format(downloadObject.getLastRecordReadingDate());
        String msg="~";
        int timeDiff=(int) (new Date().getTime()-downloadObject.getLastRecordReadingDate().getTime());
        Log.d("XXX","Time difference: "+timeDiff);
        if (timeDiff<60000) {
            msg += "Now";
        }else if (timeDiff>60000 && timeDiff<3600000){
            msg += String.valueOf((timeDiff/1000)/60);
            msg += "m";
        }else if (timeDiff>3600000 && timeDiff<86400000){
            msg += String.valueOf(((timeDiff/1000)/60)/24);
            msg += "h";
        }else if (timeDiff>86400000 && timeDiff<604800000){
            msg += String.valueOf((((timeDiff/1000)/60)/24)/7);
            msg += "w";
        }else {
            msg=new SimpleDateFormat("HH:mm:ss MM/dd").format(downloadObject.getLastRecordReadingDate());
        }
        msg+="\n"+new SimpleDateFormat("HH:mm:ss MM/dd").format(downloadObject.getLastRecordReadingDate());
        downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,msg),Conditions.NONE);
//        msg="Download: ~";
//        timeDiff=(int) (new Date().getTime()-downloadObject.getDownloadDate().getTime());
//        Log.d("XXX","Time difference: "+timeDiff);
//        if (timeDiff<60000) {
//            msg += "Now";
//        }else if (timeDiff>60000 && timeDiff<3600000){
//            msg += String.valueOf((timeDiff/1000)/60);
//            msg += "m";
//        }else if (timeDiff>3600000 && timeDiff<86400000){
//            msg += String.valueOf(((timeDiff/1000)/60)/24);
//            msg += "h";
//        }else if (timeDiff>86400000 && timeDiff<604800000){
//            msg += String.valueOf((((timeDiff/1000)/60)/24)/7);
//            msg += "w";
//        }else {
//            msg=new SimpleDateFormat("HH:mm:ss MM/dd").format(downloadObject.getLastRecordReadingDate());
//        }
//        downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,msg),Conditions.NONE);
    }
}