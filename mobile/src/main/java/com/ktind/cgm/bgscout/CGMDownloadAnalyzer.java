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
        super.analyze();
        checkDownloadStatus();
        checkRecordAge();
        checkUploaderBattery();
        checkCGMBattery();
        checkThresholdholds();
        checkLastRecordTime();
        correlateMessages();
//        downloadObject.deDup();
        return this.downloadObject;
    }

    protected void checkUploaderBattery(){
        // FIXME this breaks i18n possibilties
        String verb=(downloadObject.getConditions().contains(Conditions.STALEDATA))?"was":"is";
        if (downloadObject.getUploaderBattery() < UPLOADERBATTERYCRITICAL){
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "Uploader battery " + verb + " critically low: " + (int) downloadObject.getUploaderBattery(), Conditions.UPLOADERCRITICALLOW));
        }else if (downloadObject.getUploaderBattery() < UPLOADERBATTERYWARN) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.WARN, "Uploader battery "+verb+" low: "+(int) downloadObject.getUploaderBattery(),Conditions.UPLOADERLOW));
        }
    }

    protected void checkCGMBattery(){
        if (downloadObject.getStatus()==DownloadStatus.SUCCESS) {
            String verb=(downloadObject.getConditions().contains(Conditions.STALEDATA))?"was":"is";
            if (downloadObject.getDeviceBattery() < DEVICEBATTERYCRITICAL) {
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "CGM battery " + verb + " critically low" + downloadObject.getDeviceBattery(), Conditions.DEVICECRITICALLOW));
            }else if (downloadObject.getDeviceBattery() < DEVICEBATTERYWARN) {
                downloadObject.addMessage(new AlertMessage(AlertLevels.WARN, "CGM battery "+verb+" low: "+downloadObject.getDeviceBattery(),Conditions.DEVICELOW));
            }
        }
    }

    protected void checkRecordAge(){
        Long recordAge= null;
        Long downloadAge=new Date().getTime() - downloadObject.getDownloadDate().getTime();
        try {
            recordAge = new Date().getTime() - downloadObject.getLastRecordReadingDate().getTime();
            // Cutdown on clutter in the notification bar...
            // Only show the message for a missed reading or that the uploader isn't communicating
            if (recordAge > MAXRECORDAGE && downloadAge <= MAXRECORDAGE) {
                //FIXME if the record is over a month old then it will only show the date and it won't make sense to the user. Need to add a special condition.
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "CGM out of range/missed reading for " + TimeTools.getTimeDiffStr(downloadObject.getLastRecordReadingDate(),new Date()), Conditions.MISSEDREADING));
//                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "CGM out of range or missed reading for more than " + ((recordAge / 1000) / 60) + " minutes old", Conditions.MISSEDREADING));
            }
        } catch (NoDataException e) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.WARN,"Download did not contain any data",Conditions.NODATA));
        }
        if (downloadAge > MAXRECORDAGE)
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Uploader inactive for "+TimeTools.getTimeDiffStr(downloadObject.getDownloadDate(),new Date()),Conditions.STALEDATA));
    }

    protected void checkDownloadStatus(){
        DownloadStatus status=downloadObject.getStatus();
        switch (status){
            case DEVICENOTFOUND:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"No CGM device found",Conditions.DEVICEDISCONNECTED));
                break;
            case IOERROR:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unable to read or write to the CGM",Conditions.DOWNLOADFAILED));
                break;
            case NODATA:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"No data in download",Conditions.NODATA));
                break;
            case APPLICATIONERROR:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unknown application error",Conditions.UNKNOWN));
                break;
            case UNKNOWN:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unknown error while trying to retrieve data from CGM",Conditions.UNKNOWN));
                break;
            case REMOTEDISCONNECTED:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unable to connect to remote devices",Conditions.REMOTEDISCONNECTED));
            default:
                break;
        }
    }

    protected void checkThresholdholds(){
        try {
            int egv=0;
            egv = downloadObject.getLastReading();
            Trend trend=downloadObject.getLastTrend();
            GlucoseUnit unit=downloadObject.getUnit();
            AlertLevels alertLevel=AlertLevels.INFO;
            Conditions condition=Conditions.INRANGE;
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
            String preamble=(downloadObject.getConditions().contains(Conditions.MISSEDREADING) || downloadObject.getConditions().contains(Conditions.STALEDATA))?"Last reading: ":"";
            downloadObject.addMessage(new AlertMessage(alertLevel,preamble+egv+" "+unit+" "+trend,condition));
        } catch (NoDataException e) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.WARN,"Download did not contain any data",Conditions.NODATA));
        }
    }

    protected void checkLastRecordTime(){
        String msg;
        try {
            msg=TimeTools.getTimeDiffStr(downloadObject.getLastRecordReadingDate(),new Date());
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,msg,Conditions.READINGTIME));
        } catch (NoDataException e) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.WARN,"Download did not contain any data",Conditions.NODATA));
        }
    }

    @Override
    protected void correlateMessages(){
        if (downloadObject.getConditions().contains(Conditions.NODATA) && downloadObject.getConditions().contains(Conditions.DEVICEDISCONNECTED))
            downloadObject.removeMessageByCondition(Conditions.NODATA);
        if (downloadObject.getConditions().contains(Conditions.MISSEDREADING))
            downloadObject.removeMessageByCondition(Conditions.READINGTIME);
    }

}