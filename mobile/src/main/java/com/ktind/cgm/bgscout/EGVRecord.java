package com.ktind.cgm.bgscout;

//import android.util.Log;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.Date;

/**
 * Created by klee24 on 8/2/14.
 */
public class EGVRecord implements Parcelable {
    private static final String TAG = EGVRecord.class.getSimpleName();
    protected int egv;
    protected Date date;
    protected Trend trend=Trend.NONE;
    protected GlucoseUnit unit=GlucoseUnit.MGDL;
    protected boolean isNew=true;

    EGVRecord(int egv,Date date,Trend trend,boolean isNew){
        super();
        this.setEgv(egv);
        this.setDate(date);
        this.setTrend(trend);
        this.setNew(isNew);
    }



    public EGVRecord(){
        super();
    }

    public GlucoseUnit getUnit() {
        return unit;
    }

    public void setUnit(GlucoseUnit unit) {
        this.unit = unit;
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(egv);
        dest.writeLong(date.getTime());
        dest.writeInt(trend.getVal());
        dest.writeInt(unit.getValue());
        dest.writeByte((byte) (isNew ? 1 : 0));
    }

    public static final Parcelable.Creator<EGVRecord> CREATOR
            = new Parcelable.Creator<EGVRecord>() {
        public EGVRecord createFromParcel(Parcel in) {
            return new EGVRecord(in);
        }

        public EGVRecord[] newArray(int size) {
            return new EGVRecord[size];
        }
    };

    private EGVRecord(Parcel in) {
        egv=in.readInt();
        date=new Date(in.readLong());
        trend=Trend.values()[in.readInt()];
        unit=GlucoseUnit.values()[in.readInt()];
        isNew = in.readByte() != 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ((Object) this).getClass() != o.getClass()) return false;

        EGVRecord record = (EGVRecord) o;

        if (egv != record.egv){
            Log.d(TAG, "EGV Record failed on comparison egv");
            return false;
        }
        if (isNew != record.isNew){
            Log.d(TAG, "EGV Record failed on comparison isNew");
            return false;
        }
        if (!date.equals(record.date)){
            Log.d(TAG, "EGV Record failed on comparison date");
            return false;
        }
        if (trend != record.trend){
            Log.d(TAG, "EGV Record failed on comparison trend");
            return false;
        }
        if (unit != record.unit){
            Log.d(TAG, "EGV Record failed on comparison unit");
            return false;
        }
//        return (unit != record.unit);

        return true;
    }

    @Override
    public int hashCode() {
        int result = egv;
        result = 31 * result + date.hashCode();
        result = 31 * result + trend.hashCode();
        result = 31 * result + unit.hashCode();
        result = 31 * result + (isNew ? 1 : 0);
        return result;
    }
}
