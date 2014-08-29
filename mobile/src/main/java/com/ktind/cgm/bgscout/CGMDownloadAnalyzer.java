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
import android.preference.PreferenceManager;

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
    protected final int MAXRECORDAGE=300000;
    protected EGVLimits egvLimits=new EGVLimits();
//    protected EGVThresholdsEnum conditions=EGVThresholdsEnum.INRANGE;
    

    CGMDownloadAnalyzer(DownloadObject dl,Context context) {
        super(dl);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        EGVThresholds warnThreshold=new EGVThresholds();
        EGVThresholds criticalThreshold=new EGVThresholds();

        warnThreshold.setLowThreshold(Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_low_threshold", "60")));
        warnThreshold.setHighThreshold(Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_high_threshold", "180")));

        // FIXME Are these default values set properly?
        criticalThreshold.setLowThreshold(Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_critical_low_threshold", "50")));
        criticalThreshold.setHighThreshold(Integer.valueOf(sharedPref.getString(dl.getDeviceID() + "_critical_high_threshold", "300")));

        egvLimits.setWarnThreshold(warnThreshold);
        egvLimits.setCriticalThreshold(criticalThreshold);
    }

    public AnalyzedDownload analyze() {
        try {
            super.analyze();
            checkUploaderBattery();
            checkCGMBattery();
            checkRecordAge();
            checkDownloadStatus();
            checkThresholdholds();
            checkLastRecordTime();
        } catch (NoDataException e) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.WARN,"Download did not contain any data"),Conditions.NODATA);
            e.printStackTrace();
        }
        return this.downloadObject;
    }

    protected void checkUploaderBattery(){
        if (downloadObject.getUploaderBattery() < UPLOADERBATTERYWARN) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.WARN, "Uploader battery is low"),Conditions.UPLOADERLOW);
        } else if (downloadObject.getUploaderBattery() < UPLOADERBATTERYCRITICAL) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "Uploader battery is critically low"),Conditions.UPLOADERCRITICALLOW);
        }
    }

    protected void checkCGMBattery(){
        if (downloadObject.getStatus()==DownloadStatus.SUCCESS) {
            if (downloadObject.getDeviceBattery() < DEVICEBATTERYWARN) {
                downloadObject.addMessage(new AlertMessage(AlertLevels.WARN, "CGM battery is low"),Conditions.DEVICELOW);
            } else if (downloadObject.getDeviceBattery() < DEVICEBATTERYCRITICAL) {
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "CGM battery is critically low"),Conditions.DEVICECRITICALLOW);
            }
        }
    }

    protected void checkRecordAge() throws NoDataException {
        Long recordAge=new Date().getTime() - downloadObject.getLastRecordReadingDate().getTime();
        if (recordAge > MAXRECORDAGE)
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Last record is greater than "+((recordAge/1000)/60)+" minutes old"),Conditions.STALEDATA);
    }

    protected void checkDownloadStatus(){
        DownloadStatus status=downloadObject.getStatus();
        switch (status){
            case DEVICENOTFOUND:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"No CGM device found"),Conditions.DEVICEDISCONNECTED);
                break;
            case NODATA:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"No data in download"),Conditions.NODATA);
                break;
            case APPLICATIONERROR:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unknown application error"),Conditions.UNKNOWN);
                break;
            case IOERROR:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unable to read or write to the CGM"),Conditions.DOWNLOADFAILED);
                break;
            case UNKNOWN:
                downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,"Unknown error while trying to retrieve data from CGM"),Conditions.UNKNOWN);
                break;
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
        String msg=new SimpleDateFormat("HH:mm:ss MM/dd").format(downloadObject.getLastRecordReadingDate());
        downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL,msg),Conditions.NONE);
    }
}