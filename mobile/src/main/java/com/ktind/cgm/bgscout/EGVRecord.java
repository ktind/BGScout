package com.ktind.cgm.bgscout;

import android.util.Log;

import java.util.Date;

/**
 * Created by klee24 on 8/2/14.
 */
public class EGVRecord {
    private static final String TAG = EGVRecord.class.getSimpleName();
    protected int egv;
    protected Date date;
    protected Trend trend;
    protected boolean isNew;

    EGVRecord(int egv,Date date,Trend trend,boolean isNew){
        super();
        this.setEgv(egv);
        this.setDate(date);
        this.setTrend(trend);
        this.setNew(isNew);
    }

    EGVRecord(){
        super();
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

    public Trend getTrend() {
        return trend;
    }

    public void setTrend(Trend trend) {
        this.trend = trend;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }
}
