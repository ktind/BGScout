package com.ktind.cgm.bgscout;

import android.util.Log;

import com.ktind.cgm.bgscout.DexcomG4.G4EGVSpecialValue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by klee24 on 8/2/14.
 */
public class DownloadObject {
    private static final String TAG = DownloadObject.class.getSimpleName();
    private AbstractDevice device;
    private String deviceName;
    private boolean isRemoteDevice;
    private EGVRecord[] egvRecords;
    private DownloadStatus status;
    private String specialValueMessage=null;
    private int deviceBattery;
    private float uploaderBattery;
    private GlucoseUnit unit;
    private ArrayList<HashMap<AlertLevels,String>> alertMessages=new ArrayList<HashMap<AlertLevels, String>>();
    private HashMap<String, String> downloadMessage=new HashMap<String, String>();
    private String deviceID;
    private Date lastReadingDate;

    public DownloadObject(){
        egvRecords=new EGVRecord[0];
        device=null;
        status=DownloadStatus.NONE;
        specialValueMessage=G4EGVSpecialValue.NONE.toString();
    }

    // FIXME should we really have to pass the entire device when we just need the ID String?
    public DownloadObject(AbstractDevice device, EGVRecord[] egvRecords, DownloadStatus downloadStatus){
        super();
        setDeviceID(device.getDeviceIDStr());
//        setDevice(device);
        setEgvRecords(egvRecords);
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

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
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
        return egvRecords;
    }

    public DownloadObject setEgvRecords(EGVRecord[] egvRecords) {
        this.egvRecords = egvRecords;
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
        if (egvRecords==null || egvRecords.length==0)
            throw new NoDataException("There are no records in the previous download");
        return egvRecords[egvRecords.length-1];
    }

    public Date getLastReadingDate() {
        // First get shared preferences
        // Then check to see if there are any records
        // finally set to now - 2.5 hours.
        if (this.lastReadingDate!=null)
            return this.lastReadingDate;
        try {
            if (this.egvRecords != null && this.egvRecords.length > 0)
                return getLastRecord().getDate();
        } catch (NoDataException e) {
            Log.d(TAG,"No previous downloads");
        }
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

    public void buildMessage(){
        downloadMessage.put("device","dexcom");
        downloadMessage.put("uploaderBattery", String.valueOf(getUploaderBattery()));
        downloadMessage.put("downloadStatus",String.valueOf(getStatus().getVal()));
        downloadMessage.put("specialMessage",getSpecialValueMessage());
        downloadMessage.put("name",getDeviceName());
        downloadMessage.put("isRemoteDevice",String.valueOf(isRemoteDevice()));
        downloadMessage.put("deviceID",getDeviceID());
        downloadMessage.put("date", String.valueOf(getLastReadingDate().getTime()));
        try {
            downloadMessage.put("sgv",String.valueOf(getLastReading()));
        } catch (NoDataException e) {
            e.printStackTrace();
        }
        downloadMessage.put("unit",String.valueOf(getUnit().getValue()));
        try {
            downloadMessage.put("direction",String.valueOf(getLastTrend().getNsString()));
        } catch (NoDataException e) {
            e.printStackTrace();
        }
        downloadMessage.put("deviceBattery",String.valueOf(getDeviceBattery()));
    }

    public DownloadObject buildFromJSON(String json){
        JSONObject jsonObj=null;
        try {
            jsonObj=new JSONObject(json);
        } catch (JSONException e) {
            Log.e(TAG, "Problem parsing json: "+json);
            e.printStackTrace();
        }
        return buildFromJSON(jsonObj);
    }

    public DownloadObject buildFromJSON(JSONObject jsonObject) {
        DownloadObject result = new DownloadObject();
        Log.v(TAG,"Checking uploaderBattery");
        result.setUploaderBattery(jsonObject.optInt("uploaderBattery",-1));
        Log.v(TAG,"Checking downloadStatus");
        result.setStatus(DownloadStatus.values()[jsonObject.optInt("downloadStatus", DownloadStatus.NONE.getVal())]);
        Log.v(TAG,"Checking specialMessage");
        result.setSpecialValueMessage(jsonObject.optString("specialMessage", ""));
        Log.v(TAG,"Checking name");
        result.setDeviceName(jsonObject.optString("name", "???"));
        Log.v(TAG,"Checking unit");
        result.setUnit(GlucoseUnit.values()[jsonObject.optInt("unit", GlucoseUnit.MGDL.getValue())]);
        Log.v(TAG,"Checking deviceID");
        result.setDeviceID(jsonObject.optString("deviceID", "?"));
        Log.v(TAG,"Checking deviceBattery");
        result.setDeviceBattery(jsonObject.optInt("deviceBattery", -1));

        EGVRecord[] records = new EGVRecord[1];
        Log.v(TAG,"Checking sgv");
        int bg=jsonObject.optInt("sgv",-1);
        if (bg!=-1) {
            Trend trend = Trend.NONE;
            GlucoseUnit u = result.getUnit();
            Date d=new Date(jsonObject.optLong("date",new Date().getTime()));
            boolean n=true;
            result.getUnit();
            trend=trend.getTrendByNsString(jsonObject.optString("direction", Trend.NONE.toString()));
            EGVRecord[] recs=new EGVRecord[1];
            recs[0]=new EGVRecord(bg,d,trend,n);
            result.setEgvRecords(recs);
//            result.setEgvRecords(records);
        } else {
            Log.v(TAG,"No Readings");
        }
        return result;
    }

    public JSONObject getJson(){
        JSONObject jsonObject=new JSONObject();
        for (String key:downloadMessage.keySet()){
            try {
                jsonObject.put(key,downloadMessage.get(key));
            } catch (JSONException e) {
                if (key.equals("sgv")||key.equals("deviceBattery")||key.equals("uploaderBatter")||key.equals("name"))
                    try {
                        jsonObject.put(key, "---");
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }
                    e.printStackTrace();
            }
        }
        return jsonObject;
    }
}
