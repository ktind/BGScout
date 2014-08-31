package com.ktind.cgm.bgscout;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.ktind.cgm.bgscout.DexcomG4.G4EGVSpecialValue;


import java.util.ArrayList;
import java.util.Arrays;
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

// FIXME this object is getting too large. A full constructor would be too unwieldy. Look into breaking into smaller classes
public class DownloadObject implements Parcelable {
    protected static final String TAG = DownloadObject.class.getSimpleName();
    protected String deviceName="NOTSET";
    protected boolean isRemoteDevice;

    protected ArrayList<EGVRecord> egvRecords=new ArrayList<EGVRecord>();
    protected DownloadStatus status;
    protected String specialValueMessage=null;
    protected int deviceBattery;
    protected float uploaderBattery;
    protected GlucoseUnit unit;
//    protected ArrayList<HashMap<AlertLevels,String>> alertMessages=new ArrayList<HashMap<AlertLevels, String>>();
//    protected HashMap<String, String> downloadMessage=new HashMap<String, String>();
    protected String deviceID;
    protected Date lastReadingDate;
    protected String driver;
    private Date downloadDate;

    public Date getDownloadDate() {
        return downloadDate;
    }

    public DownloadObject setDownloadDate(Date downloadDate) {
        this.downloadDate = downloadDate;
        return this;
    }

    public DownloadObject(){
        status=DownloadStatus.NONE;
        specialValueMessage=G4EGVSpecialValue.NONE.toString();
    }

    public DownloadObject(DownloadObject dl){
        deviceName=dl.deviceName;
        isRemoteDevice=dl.isRemoteDevice;
        egvRecords=dl.egvRecords;
        status=dl.status;
        specialValueMessage=dl.specialValueMessage;
        deviceBattery=dl.deviceBattery;
        uploaderBattery=dl.uploaderBattery;
        unit=dl.unit;
        deviceID=dl.deviceID;
        lastReadingDate=dl.lastReadingDate;
        driver=dl.driver;
    }
    
//    public DownloadObject(DownloadObject dl){
//        deviceName=dl.getDeviceName();
//        isRemoteDevice=dl.isRemoteDevice();
//        egvRecords=dl.egvRecords;
//        status=dl.getStatus();
//        uploaderBattery=dl.getUploaderBattery();
//        deviceBattery=dl.getDeviceBattery();
//        unit=dl.getUnit();
//        deviceID=dl.getDeviceID();
//        lastReadingDate=dl.lastReadingDate;
//        driver=dl.driver;
//    }

    public DownloadObject(AbstractDevice device, EGVRecord[] egvRecords, DownloadStatus downloadStatus){
        super();
        setDeviceID(device.getDeviceIDStr());
        setEgvRecords(new ArrayList<EGVRecord>(Arrays.asList(egvRecords)));
        setStatus(downloadStatus);
    }


    public GlucoseUnit getUnit() {
        return unit;
    }

    public DownloadObject setUnit(GlucoseUnit unit) {
        this.unit = unit;
        return this;
    }

    public String getDeviceID() {
        return deviceID;
    }

    public DownloadObject setDeviceID(String deviceID) {
        this.deviceID = deviceID;
        return this;
    }

    public boolean isRemoteDevice() {
        return isRemoteDevice;
    }

    public DownloadObject setRemoteDevice(boolean isRemoteDevice) {
        this.isRemoteDevice = isRemoteDevice;
        return this;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public DownloadObject setDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }

    public int getDeviceBattery() {
        return deviceBattery;
    }

    public DownloadObject setDeviceBattery(int deviceBattery) {
        this.deviceBattery = deviceBattery;
        return this;
    }

//    public DownloadObject addAlertMessages(ArrayList<HashMap<AlertLevels,String>> messages){
//        for (HashMap<AlertLevels,String> message:messages)
//            alertMessages.add(message);
//        return this;
//    }

    public float getUploaderBattery() {
        return uploaderBattery;
    }

    public DownloadObject setUploaderBattery(float uploaderBattery) {
        this.uploaderBattery = uploaderBattery;
        return this;
    }

    public String getSpecialValueMessage() {
        return specialValueMessage;
    }

    public DownloadObject setSpecialValueMessage(String specialValueMessage) {
        this.specialValueMessage = specialValueMessage;
        return this;
    }

    public EGVRecord[] getEgvRecords() {
        return egvRecords.toArray(new EGVRecord[egvRecords.size()]);
    }

    public ArrayList<EGVRecord> getEgvArrayListRecords() {
        return egvRecords;
    }


    public DownloadObject setEgvRecords(ArrayList<EGVRecord> records) {
        this.egvRecords = records;
        return this;
    }

    public DownloadObject setEgvRecords(EGVRecord[] records){
        this.egvRecords=new ArrayList<EGVRecord>(Arrays.asList(records));
        return this;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public DownloadObject setStatus(DownloadStatus status) {
        this.status = status;
        return this;
    }

//    public ArrayList<HashMap<AlertLevels, String>> getAlertMessages() {
//        return alertMessages;
//    }

    /*
        Provides us safe access to the details of the last reading.
         */
    public EGVRecord getLastRecord() throws NoDataException {
        if (egvRecords==null || egvRecords.size()==0)
            throw new NoDataException("There are no records in the download");
        return egvRecords.get(egvRecords.size()-1);
//        return egvRecords[egvRecords.length-1];
    }

    public Date getLastRecordReadingDate() throws NoDataException {
        return getLastRecord().getDate();
    }

    public Date getLastReadingDate() {
        // First get shared preferences
        // Then check to see if there are any records
        // finally set to now - 2.5 hours.
        if (this.lastReadingDate!=null)
            return this.lastReadingDate;
//        Log.e(TAG,"length=>"+egvRecords.length);
//        Log.e(TAG,"Date=>"+egvRecords[egvRecords.length-1].date);
        if (this.egvRecords != null && this.egvRecords.size() > 0)
            return egvRecords.get(egvRecords.size()-1).getDate();
//            return egvRecords[egvRecords.length-1].getDate();
        return new Date(new Date().getTime()-9000000L);
    }

    public DownloadObject setLastReadingDate(Date date){
        this.lastReadingDate=date;
        return this;
    }

//    public void trimReadingsAfter(Long afterDateLong){
//        ArrayList<EGVRecord> recs=egvRecords;
//        Log.d(TAG,"Size before trim: "+recs.size());
//        Date afterDate=new Date(afterDateLong);
//        for (Iterator<EGVRecord> iterator = recs.iterator(); iterator.hasNext(); ) {
//            EGVRecord record = iterator.next();
//            // trim anything after the date UNLESS that means we trim everything. Let's keep
//            // the last record in there just in case. Need to find a better solution to this
//            // the method doesn't reflect its purpose
//            if (! record.getDate().after(afterDate) && recs.size()>1)
//                iterator.remove();
//        }
//        Log.d(TAG,"Size after trim: "+recs.size()+" vs original "+egvRecords.size());
//    }


    public int getLastReading() throws NoDataException {
        return getLastRecord().getEgv();
    }

    public String getLastReadingString(){
        String result;
        try {
            return String.valueOf(getLastReading());
        } catch (NoDataException e) {
            return "---";
        }
    }

    public Trend getLastTrend() throws NoDataException {
        return getLastRecord().getTrend();
    }

//    public HashMap<String,String> getDownloadMessage(){
//        return downloadMessage;
//    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(deviceName);
        dest.writeByte((byte) (isRemoteDevice ? 1 : 0));
        dest.writeTypedList(egvRecords);
        dest.writeInt(status.getVal());
        dest.writeString(specialValueMessage);
        dest.writeInt(deviceBattery);
        dest.writeFloat(uploaderBattery);
        dest.writeInt(unit.getValue());
        dest.writeString(deviceID);
        dest.writeLong(lastReadingDate.getTime());
    }

    public static final Parcelable.Creator<DownloadObject> CREATOR
            = new Parcelable.Creator<DownloadObject>() {
        public DownloadObject createFromParcel(Parcel in) {
            return new DownloadObject(in);
        }

        public DownloadObject[] newArray(int size) {
            return new DownloadObject[size];
        }
    };
    
    protected DownloadObject(Parcel in) {
        deviceName=in.readString();
        isRemoteDevice=in.readByte() != 0;
        in.readTypedList(egvRecords,EGVRecord.CREATOR);
        status=DownloadStatus.values()[in.readInt()];
        specialValueMessage=in.readString();
        deviceBattery=in.readInt();
        uploaderBattery=in.readFloat();
        unit=GlucoseUnit.values()[in.readInt()];
        deviceID=in.readString();
        lastReadingDate=new Date(in.readLong());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || ((Object) this).getClass() != o.getClass()) return false;

        DownloadObject that = (DownloadObject) o;

        if (deviceBattery != that.deviceBattery){
            Log.d(TAG, "Failed comparison on device battery");
            return false;
        }
        if (isRemoteDevice != that.isRemoteDevice){
            Log.d(TAG, "Failed comparison on remote device");
            return false;
        }
        if (Float.compare(that.uploaderBattery, uploaderBattery) != 0){
            Log.d(TAG, "Failed comparison on uploader battery");
            return false;
        }
//        if (alertMessages != null ? !alertMessages.equals(that.alertMessages) : that.alertMessages != null) {
//            Log.d(TAG, "Failed comparison on alertMessages");
//            return false;
//        }
        if (!deviceID.equals(that.deviceID)) {
            Log.d(TAG, "Failed comparison on deviceID");
            return false;
        }
        if (!deviceName.equals(that.deviceName)){
            Log.d(TAG, "Failed comparison on deviceName");
            return false;
        }
//        if (downloadMessage != null ? !downloadMessage.equals(that.downloadMessage) : that.downloadMessage != null) {
//            Log.d(TAG, "Failed comparison on downloadMessage");
//            return false;
//        }
        if (egvRecords != null ? !egvRecords.equals(that.egvRecords) : that.egvRecords != null) {
            Log.d(TAG, "Failed comparison on egvRecords");
            return false;
        }
        if (lastReadingDate != null ? !lastReadingDate.equals(that.lastReadingDate) : that.lastReadingDate != null) {
            Log.d(TAG, "Failed comparison on lastReadingDate");
            return false;
        }
        if (specialValueMessage != null ? !specialValueMessage.equals(that.specialValueMessage) : that.specialValueMessage != null) {
            Log.d(TAG, "Failed comparison on specialValueMessage");
            return false;
        }
        if (status != that.status){
            Log.d(TAG, "Failed comparison on status");
            return false;
        }
        if (unit != that.unit){
            Log.d(TAG, "Failed comparison on unit");
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = deviceName.hashCode();
        result = 31 * result + (isRemoteDevice ? 1 : 0);
        result = 31 * result + (egvRecords != null ? egvRecords.hashCode() : 0);
        result = 31 * result + status.hashCode();
        result = 31 * result + (specialValueMessage != null ? specialValueMessage.hashCode() : 0);
        result = 31 * result + deviceBattery;
        result = 31 * result + (uploaderBattery != +0.0f ? Float.floatToIntBits(uploaderBattery) : 0);
        result = 31 * result + unit.hashCode();
//        result = 31 * result + (alertMessages != null ? alertMessages.hashCode() : 0);
//        result = 31 * result + (downloadMessage != null ? downloadMessage.hashCode() : 0);
        result = 31 * result + deviceID.hashCode();
        result = 31 * result + (lastReadingDate != null ? lastReadingDate.hashCode() : 0);
        return result;
    }

    public String getDriver() {
        return driver;
    }

    public DownloadObject setDriver(String driver) {
        this.driver = driver;
        return this;
    }
}
