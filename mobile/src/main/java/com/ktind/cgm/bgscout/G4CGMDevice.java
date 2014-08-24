package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.ktind.cgm.bgscout.DexcomG4.G4;
import com.ktind.cgm.bgscout.DexcomG4.G4EGVRecord;
import com.ktind.cgm.bgscout.DexcomG4.G4EGVSpecialValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;


/**
 * Created by klee24 on 8/2/14.
 */
public class G4CGMDevice extends AbstractPollDevice {
    protected String serialNum;
    protected String receiverID;
    protected int cgmBattery=-1;
    private static final String TAG = G4CGMDevice.class.getSimpleName();
    G4 device;

    public G4CGMDevice(String name,int devID,Context appContext,Handler mH){
        super(name,devID,appContext,mH);
        device=new G4(appContext);
        remote = false;
//        cgmTransport=new G4USBSerialTransport(appContext);
        this.deviceType="Dexcom G4";
    }

    @Override
    public int getDeviceBattery() throws OperationNotSupportedException, NoDeviceFoundException, DeviceIOException {
        if (cgmBattery==-1)
            cgmBattery=device.getBatteryLevel();
        return cgmBattery;
    }

    @Override
    public void connect() throws IOException, DeviceException {
        device.connect();
    }

    @Override
    protected DownloadObject doDownload() {
        String specialMessage="";
        int deviceBattery=-1;
        float uploaderBattery=getUploaderBattery()*100.0f;

        DownloadStatus status=DownloadStatus.NONE;
        ArrayList<EGVRecord> egvList=new ArrayList<EGVRecord>();
        ArrayList<HashMap<AlertLevels,String>> alerts=new ArrayList<HashMap<AlertLevels,String>>();

        int unitID=Integer.parseInt(sharedPref.getString(deviceIDStr+"_units","0"));
        GlucoseUnit g_unit;
        g_unit=GlucoseUnit.values()[unitID];
        if (g_unit==GlucoseUnit.NONE)
            g_unit=GlucoseUnit.MGDL;
        try {
            connect();
            egvList = G4RecordAdapter.convertToEGVRecordArrayList((ArrayList<G4EGVRecord>) device.getLastRecords());
            Log.d(TAG,"Display Time: "+device.getDisplayTime()+ " Current time: "+new Date());
            if (sharedPref.getBoolean(deviceIDStr+"_time_sync",true))
                syncTime();
            if (g_unit==null)
                getUnit();
            deviceBattery = this.getDeviceBattery();
            disconnect();
            batteryBalance(deviceBattery,uploaderBattery);

            status = DownloadStatus.SUCCESS;

            if (egvList.size()>0) {
                int lastBG = egvList.get(egvList.size()-1).getEgv();
                for (G4EGVSpecialValue specialValue : G4EGVSpecialValue.values()) {
                    if (lastBG == specialValue.getValue()) {
                        status = DownloadStatus.SPECIALVALUE;
                        specialMessage=G4EGVSpecialValue.getEGVSpecialValue(lastBG).toString();
                        break;
                    }
                }
            } else {
                status=DownloadStatus.NODATA;
            }
        } catch (OperationNotSupportedException e) {
            Log.e(TAG,"Application error",e);
            status=DownloadStatus.APPLICATIONERROR;
        } catch (DeviceIOException e) {
            Log.e(TAG,"Unable to read/write to the device",e);
            status=DownloadStatus.IOERROR;
        } catch (NoDeviceFoundException e) {
            status=DownloadStatus.DEVICENOTFOUND;
        } catch (DeviceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        HashMap<AlertLevels, String> alert=new HashMap<AlertLevels,String>();
//        if (uploaderBattery<20.0f) {
//            alert.put(AlertLevels.WARN,"Low battery on uploader");
//            alerts.add(alert);
//        } else if (uploaderBattery<10.0f) {
//            alert.put(AlertLevels.CRITICAL,"Low battery on uploader");
//            alerts.add(alert);
//        }
//
//        if (deviceBattery<20) {
//            alert.put(AlertLevels.WARN,"Low battery on Dexcom G4");
//            alerts.add(alert);
//        } else if (deviceBattery<10) {
//            alert.put(AlertLevels.CRITICAL,"Low battery on Dexcom G4");
//            alerts.add(alert);
//        }
        DownloadObject ddo=new DownloadObject();
        // Default to the last 2.5 hours at max - it may be less due to the way that the library pulls the data from the
        // device. The reason for this is the way the device stores the records it depends on the timing and page boundaries
        Long lastReadingDate=sharedPref.getLong(deviceIDStr+"_lastG4Download",new Date(new Date().getTime()-9000000L).getTime());
        Long lastReadingDateRecord;
        SharedPreferences.Editor editor = sharedPref.edit();
        // Filter
        Log.d(TAG,"egvList: "+egvList.size());
        egvList=filter(lastReadingDate, egvList);
        // Then set the new last reading date
        if (egvList!=null && egvList.size()>0)
            lastReadingDateRecord=egvList.get(egvList.size()-1).getDate().getTime();
        else
            lastReadingDateRecord=new Date().getTime()-9000000L;
        // FIXME this is an awkward transition from Array to ArrayList. There is too much converting and casting going on.
        if (egvList==null)
            egvList=new ArrayList<EGVRecord>();
        // ddo => Device Download Object..
        ddo.setDeviceBattery(deviceBattery)
                .setLastReadingDate(new Date(lastReadingDateRecord))
                .setUploaderBattery(uploaderBattery)
                .setDeviceName(name)
                .setUnit(g_unit)
                .setStatus(status)
                .setEgvRecords(egvList)
                .setSpecialValueMessage(specialMessage)
                .addAlertMessages(alerts);
        editor.putLong(deviceIDStr+"_lastG4Download",lastReadingDateRecord);
        editor.apply();
        setLastDownloadObject(ddo);
        return ddo;
    }

    public ArrayList<EGVRecord> filter(Long afterDateLong,ArrayList<EGVRecord> egvRecords){
        Log.d(TAG,"in Filter egvList: "+egvRecords.size());
        Date afterDate=new Date(afterDateLong);
        for (Iterator<EGVRecord> iterator = egvRecords.iterator(); iterator.hasNext(); ) {
            EGVRecord record = iterator.next();
            if (! record.getDate().after(afterDate))
                iterator.remove();
        }

        return egvRecords;
    }

    public void syncTime(){
        if (!device.isConnected())
            return;
        try {
            Date dispDate=device.getDisplayTime();
            Long jitter=dispDate.getTime()-new Date().getTime();
            if (Math.abs(jitter) > Constants.TIMEDRIFTOLERANCE ) {
                Log.w(TAG,"Device time off by "+jitter+" ms");
                device.syncTimeToDevice();
            }
        }catch (DeviceException e){
            Log.e(TAG,"Unable to syncTime to device");
        }
    }

    private void batteryBalance(int deviceBattery,float uploaderBattery){
        if (!device.isConnected())
            return;
        Log.d(TAG, "Device battery level: " + deviceBattery);
        Log.d(TAG, "Phone battery level: " + uploaderBattery);
        if (deviceBattery < 40) {
            if (uploaderBattery > 0.40) {
                Log.d(TAG, "Setting phone to charge device");
                device.setChargeDevice(true);
            } else {
                Log.d(TAG, "G4 battery low but this device is too low to charge it");
                device.setChargeDevice(false);
            }
        } else {
            Log.d(TAG, "Preventing this device from charging G4");
            device.setChargeDevice(false);
        }
    }
    @Override
    public void disconnect() {
        device.disconnect();
    }
}