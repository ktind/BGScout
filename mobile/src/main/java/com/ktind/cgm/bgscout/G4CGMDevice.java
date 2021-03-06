package com.ktind.cgm.bgscout;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.ktind.cgm.bgscout.DexcomG4.G4;
import com.ktind.cgm.bgscout.DexcomG4.G4EGVRecord;

import org.acra.ACRA;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;


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
public class G4CGMDevice extends AbstractPollDevice {
    protected int cgmBattery=-1;

    private static final String TAG = G4CGMDevice.class.getSimpleName();
    G4 device;

    public G4CGMDevice(String name,int devID,Context context){
        super(name,devID,context,"DexcomG4");
        device=new G4(context);
        remote = false;
        this.deviceType="Dexcom G4";
    }

    @Override
    public int getDeviceBattery() throws OperationNotSupportedException, NoDeviceFoundException, DeviceIOException {
        cgmBattery=device.getBatteryLevel();
        return cgmBattery;
    }

    @Override
    public void connect() throws NoDeviceFoundException, OperationNotSupportedException, DeviceIOException {
        device.connect();
    }

    @Override
    public void start() {
        super.start();
        state=State.STARTED;
    }

    @Override
    protected DownloadObject doDownload() {
        int deviceBattery=-1;
        float uploaderBattery=getUploaderBattery()*100.0f;

        DownloadStatus status=DownloadStatus.NONE;
        ArrayList<EGVRecord> egvList=new ArrayList<EGVRecord>();
        ArrayList<HashMap<AlertLevels,String>> alerts=new ArrayList<HashMap<AlertLevels,String>>();

        int unitID=Integer.parseInt(sharedPref.getString(deviceIDStr+"_units","0"));
        GlucoseUnit g_unit;
        g_unit=GlucoseUnit.values()[unitID];
        Tracker t = ((BGScout) context.getApplicationContext()).getTracker();
        if (g_unit==GlucoseUnit.NONE)
            g_unit=GlucoseUnit.MGDL;
        try {
            device.connect();
            device.setup();

            egvList = G4RecordAdapter.convertToEGVRecordArrayList((ArrayList<G4EGVRecord>) device.getLastRecords());
            Log.d(TAG,"Display Time: "+device.getDisplayTime()+ " Current time: "+new Date());
            if (sharedPref.getBoolean(deviceIDStr+"_time_sync",true))
                syncTime();
            if (g_unit==null)
                getUnit();
            deviceBattery = this.getDeviceBattery();
            batteryBalance(deviceBattery, uploaderBattery);
            device.disconnect();
            status = DownloadStatus.SUCCESS;
            if (egvList.size()==0){
                status=DownloadStatus.NODATA;
            }
        } catch (OperationNotSupportedException e) {
            Log.e(TAG,"Application error",e);
            ACRA.getErrorReporter().handleException(e);
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("Application error: "+e.getMessage())
                    .setFatal(false)
                    .build());
            status=DownloadStatus.APPLICATIONERROR;
        } catch (DeviceIOException e) {
            Log.e(TAG,"Unable to read/write to the device",e);
            ACRA.getErrorReporter().handleException(e);
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("IO error: "+e.getMessage())
                    .setFatal(false)
                    .build());
            status=DownloadStatus.IOERROR;
        } catch (NoDeviceFoundException e) {
            status=DownloadStatus.DEVICENOTFOUND;
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("Device not found error: "+e.getMessage())
                    .setFatal(false)
                    .build());
        }
        DownloadObject ddo=new DownloadObject();
        // Default to the last 2.5 hours at max - it may be less due to the way that the library pulls the data from the
        // device. The reason for this is the way the device stores the records it depends on the timing and page boundaries
//        Long lastReadingDate=sharedPref.getLong(deviceIDStr+"_lastG4Download",new Date(new Date().getTime()-9000000L).getTime());
        Long lastReadingDateRecord;
//        SharedPreferences.Editor editor = sharedPref.edit();
        // Filter
        Log.d(TAG,"egvList: "+egvList.size());
        // Then set the new last reading date
        if (egvList.size()>0)
            lastReadingDateRecord=egvList.get(egvList.size()-1).getDate().getTime();
        else
            lastReadingDateRecord=new Date().getTime()-9000000L;
//        if (egvList==null)
//            egvList=new ArrayList<EGVRecord>();
        // ddo => Device Download Object..
        ddo.setDeviceBattery(deviceBattery)
                .setLastReadingDate(new Date(lastReadingDateRecord))
                .setUploaderBattery(uploaderBattery)
                .setDeviceName(name)
                .setUnit(g_unit)
                .setStatus(status)
                .setEgvRecords(egvList)
                .setDriver(driver)
                .setDownloadDate(new Date());
//        editor.putLong(deviceIDStr+"_lastG4Download",lastReadingDateRecord);
//        editor.apply();
        setLastDownloadObject(ddo);
        return ddo;
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