package com.ktind.cgm.bgscout.DexcomG4;

import android.util.Log;

import com.ktind.cgm.bgscout.BitTools;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Created by klee24 on 7/13/14.
 */
public class G4EGVRecord extends G4Record {
    private static final String TAG = G4EGVRecord.class.getSimpleName();
    protected int egv;
    protected Date date;
    protected G4Trend trend=G4Trend.NONE;
    private long systemTime=0L;
    private G4EGVSpecialValue specialValue=G4EGVSpecialValue.NONE;
    private G4NoiseMode noiseMode;

    public void setSystemTime(long systemTime) {
        this.systemTime = systemTime;
    }

    public long getSystemTime() {
        return systemTime;
    }

    public void setSpecialValue(G4EGVSpecialValue specialValue) {
        this.specialValue = specialValue;
    }

    public G4EGVSpecialValue getSpecialValue() {
        return specialValue;
    }

    public void setNoiseMode(G4NoiseMode noiseMode) {
        this.noiseMode = noiseMode;
    }

    public G4NoiseMode getNoiseMode() {
        return noiseMode;
    }

    public int getEgv() {
        return egv;
    }

    public void setEgv(int egv) {
        this.egv = egv;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public G4Trend getTrend() {
        return trend;
    }

    public void setTrend(G4Trend trend) {
        this.trend = trend;
    }

    @Override
    public ArrayList<G4EGVRecord> parse(G4DBPage page) {
        ArrayList<G4EGVRecord> results=new ArrayList<G4EGVRecord>(page.PageHeader.NumberOfRecords);
        G4EGVRecord record;
        Log.d(TAG, "Processing " + page.PageHeader.NumberOfRecords + " records from page #" + page.PageHeader.PageNumber);
        for (int i=0;i<page.PageHeader.NumberOfRecords;i++){
            if (page.PageData.length>i*13){
                record=new G4EGVRecord();

                byte[] recordBuffer=new byte[13];
                System.arraycopy(page.PageData, i * 13, recordBuffer, 0, 13);
                byte[] sysTimeArray=new byte[4];
                byte[] dispTimeArray=new byte[4];
                byte[] egvwflagArray=new byte[2];
                byte[] dirnoiseArray=new byte[1];
//                byte[] crcArray=new byte[2];
                System.arraycopy(recordBuffer, 0, sysTimeArray, 0, 4);
                System.arraycopy(recordBuffer, 4, dispTimeArray, 0, 4);
                System.arraycopy(recordBuffer, 8, egvwflagArray, 0, 2);
                System.arraycopy(recordBuffer, 10, dirnoiseArray, 0, 1);
//                System.arraycopy(recordBuffer, 11, crcArray, 0, 2);
                record.setSystemTime(BitTools.byteArraytoInt(sysTimeArray));

                long dtime=(long) BitTools.byteArraytoInt(dispTimeArray)*1000;
                Calendar mCalendar = new GregorianCalendar();
                TimeZone mTimeZone = mCalendar.getTimeZone();
                long displayTimeLong=G4Constants.RECEIVERBASEDATE+dtime;
                if (mTimeZone.inDaylightTime(new Date())){
                    displayTimeLong-=3600000L;
                }
                Date displayTimeDate=new Date(displayTimeLong);
                record.setDate(displayTimeDate);

                int bgValue= BitTools.byteArraytoInt(egvwflagArray) & 0x3FF;
                // This means we've found the end
                if (bgValue==1023) {
                    Log.d(TAG,"Last reading found in this page");
                    break;
                }
                record.setEgv(bgValue);
                record.setSpecialValue(null);
//                results[i].setUnit(getUnit());
//                results[i].s(deviceUnits);
                for (G4EGVSpecialValue e: G4EGVSpecialValue.values()) {
                    if (e.getValue()==bgValue) {
                        record.setSpecialValue(G4EGVSpecialValue.getEGVSpecialValue(bgValue));
                        Log.w(TAG,"Special value set: "+record.getSpecialValue().toString());
                        break;
                    }
                }
                int trendNoise= BitTools.byteArraytoInt(dirnoiseArray);
                record.setTrend(G4Trend.values()[trendNoise & 0xF]);
                record.setNoiseMode(G4NoiseMode.getNoiseMode((byte)(trendNoise & 0xF)>>4));
                results.add(record);
                Log.v(TAG,"Reading time("+i+"): "+record.getDate().toString()+" EGV: "+record.getEgv()+" Trend: "+record.getTrend().toString()+" Noise: "+record.getNoiseMode().toString());
            } else {
                Log.w(TAG,"Record ("+i+") appears to be truncated in page number "+page.PageHeader.PageNumber);
            }
        }
        return results;
    }
}