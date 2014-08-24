package com.ktind.cgm.bgscout;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.ktind.cgm.bgscout.DexcomG4.G4EGVSpecialValue;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by klee24 on 8/2/14.
 */
public class DownloadObject implements Parcelable {
    private static final String TAG = DownloadObject.class.getSimpleName();
    private String deviceName="NOTSET";
    private boolean isRemoteDevice;

    private ArrayList<EGVRecord> egvRecords=new ArrayList<EGVRecord>();
    private DownloadStatus status;
    private String specialValueMessage=null;
    private int deviceBattery;
    private float uploaderBattery;
    private GlucoseUnit unit;
    private ArrayList<HashMap<AlertLevels,String>> alertMessages=new ArrayList<HashMap<AlertLevels, String>>();
    private HashMap<String, String> downloadMessage=new HashMap<String, String>();
    private String deviceID;
    private Date lastReadingDate;
    private Date downloadDate=new Date();

    public DownloadObject(){
        status=DownloadStatus.NONE;
        specialValueMessage=G4EGVSpecialValue.NONE.toString();
    }


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

    public void setDeviceID(String deviceID) {
        this.deviceID = deviceID;
    }

    public boolean isRemoteDevice() {
        return isRemoteDevice;
    }

    public void setRemoteDevice(boolean isRemoteDevice) {
        this.isRemoteDevice = isRemoteDevice;
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

    public DownloadObject addAlertMessages(ArrayList<HashMap<AlertLevels,String>> messages){
        for (HashMap<AlertLevels,String> message:messages)
            alertMessages.add(message);
        return this;
    }

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

    public ArrayList<HashMap<AlertLevels, String>> getAlertMessages() {
        return alertMessages;
    }

    /*
        Provides us safe access to the details of the last reading.
         */
    public EGVRecord getLastRecord() throws NoDataException {
        if (egvRecords==null || egvRecords.size()==0)
            throw new NoDataException("There are no records in the previous download");
        return egvRecords.get(egvRecords.size()-1);
//        return egvRecords[egvRecords.length-1];
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

    public HashMap<String,String> getDownloadMessage(){
        return downloadMessage;
    }

//    public void buildMessage(){
//        downloadMessage.put("device","dexcom");
//        downloadMessage.put("uploaderBattery", String.valueOf(getUploaderBattery()));
//        downloadMessage.put("downloadStatus",String.valueOf(getStatus().getVal()));
//        downloadMessage.put("specialMessage",getSpecialValueMessage());
//        downloadMessage.put("name",getDeviceName());
//        downloadMessage.put("isRemoteDevice",String.valueOf(isRemoteDevice()));
//        downloadMessage.put("deviceID",getDeviceID());
//        downloadMessage.put("date", String.valueOf(getLastReadingDate().getTime()));
//        try {
//            downloadMessage.put("sgv",String.valueOf(getLastReading()));
//        } catch (NoDataException e) {
//            e.printStackTrace();
//        }
//        downloadMessage.put("unit",String.valueOf(getUnit().getValue()));
//        try {
//            downloadMessage.put("direction",String.valueOf(getLastTrend().getNsString()));
//        } catch (NoDataException e) {
//            e.printStackTrace();
//        }
//        downloadMessage.put("deviceBattery",String.valueOf(getDeviceBattery()));
//    }
//
//    public DownloadObject buildFromJSON(String json){
//        JSONObject jsonObj=null;
//        try {
//            jsonObj=new JSONObject(json);
//        } catch (JSONException e) {
//            Log.e(TAG, "Problem parsing json: "+json);
//            e.printStackTrace();
//        }
//        return buildFromJSON(jsonObj);
//    }
//
//    public DownloadObject buildFromJSON(JSONObject jsonObject) {
//        DownloadObject result = new DownloadObject();
//        Log.v(TAG,"Checking uploaderBattery");
//        result.setUploaderBattery(jsonObject.optInt("uploaderBattery",-1));
//        Log.v(TAG,"Checking downloadStatus");
//        result.setStatus(DownloadStatus.values()[jsonObject.optInt("downloadStatus", DownloadStatus.NONE.getVal())]);
//        Log.v(TAG,"Checking specialMessage");
//        result.setSpecialValueMessage(jsonObject.optString("specialMessage", ""));
//        Log.v(TAG,"Checking name");
//        result.setDeviceName(jsonObject.optString("name", "???"));
//        Log.v(TAG,"Checking unit");
//        result.setUnit(GlucoseUnit.values()[jsonObject.optInt("unit", GlucoseUnit.MGDL.getValue())]);
//        Log.v(TAG,"Checking deviceID");
//        result.setDeviceID(jsonObject.optString("deviceID", "?"));
//        Log.v(TAG,"Checking deviceBattery");
//        result.setDeviceBattery(jsonObject.optInt("deviceBattery", -1));
//
//        EGVRecord[] records = new EGVRecord[1];
//        Log.v(TAG,"Checking sgv");
//        int bg=jsonObject.optInt("sgv",-1);
//        if (bg!=-1) {
//            Trend trend = Trend.NONE;
//            GlucoseUnit u = result.getUnit();
//            Date d=new Date(jsonObject.optLong("date",new Date().getTime()));
//            boolean n=true;
//            result.getUnit();
//            trend=trend.getTrendByNsString(jsonObject.optString("direction", Trend.NONE.toString()));
//            EGVRecord[] recs=new EGVRecord[1];
//            recs[0]=new EGVRecord(bg,d,trend,n);
//            result.setEgvRecords(recs);
////            result.setEgvRecords(records);
//        } else {
//            Log.v(TAG,"No Readings");
//        }
//        return result;
//    }
//
//    public JSONObject getJson(){
//        JSONObject jsonObject=new JSONObject();
//        for (String key:downloadMessage.keySet()){
//            try {
//                jsonObject.put(key,downloadMessage.get(key));
//            } catch (JSONException e) {
//                if (key.equals("sgv")||key.equals("deviceBattery")||key.equals("uploaderBatter")||key.equals("name"))
//                    try {
//                        jsonObject.put(key, "---");
//                    } catch (JSONException e1) {
//                        e1.printStackTrace();
//                    }
//                    e.printStackTrace();
//            }
//        }
//        return jsonObject;
//    }
//
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

    private DownloadObject(Parcel in) {
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
        if (alertMessages != null ? !alertMessages.equals(that.alertMessages) : that.alertMessages != null) {
            Log.d(TAG, "Failed comparison on alertMessages");
            return false;
        }
        if (!deviceID.equals(that.deviceID)) {
            Log.d(TAG, "Failed comparison on deviceID");
            return false;
        }
        if (!deviceName.equals(that.deviceName)){
            Log.d(TAG, "Failed comparison on deviceName");
            return false;
        }
        if (downloadMessage != null ? !downloadMessage.equals(that.downloadMessage) : that.downloadMessage != null) {
            Log.d(TAG, "Failed comparison on downloadMessage");
            return false;
        }
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
        result = 31 * result + (alertMessages != null ? alertMessages.hashCode() : 0);
        result = 31 * result + (downloadMessage != null ? downloadMessage.hashCode() : 0);
        result = 31 * result + deviceID.hashCode();
        result = 31 * result + (lastReadingDate != null ? lastReadingDate.hashCode() : 0);
        return result;
    }
}
