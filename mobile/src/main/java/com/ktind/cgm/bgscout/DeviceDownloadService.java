package com.ktind.cgm.bgscout;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
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
public class DeviceDownloadService extends Service {
    private static final String TAG = DeviceDownloadService.class.getSimpleName();
    private ArrayList<AbstractDevice> cgms=new ArrayList<AbstractDevice>();
    private Notification.Builder notificationBuilder;
    private Handler mHandler=new Handler();
    IBinder mBinder=new LocalBinder();

    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String[] device_list={"device_1","device_2","device_3","device_4"};
        int devCount=1;
        for (String dev:device_list) {
            boolean enabled = sharedPref.getBoolean(dev+"_enable", false);
            if (enabled){
                String name = sharedPref.getString(dev+"_name","");
                int type = Integer.parseInt(sharedPref.getString(dev + "_type", "0"));
                AbstractDevice cgm=null;
                switch(type){
                    case 0:
                        cgm=new G4CGMDevice(name,devCount,getBaseContext(),mHandler);
                        break;
                    case 1:
                        cgm=new RemoteMongoDevice(name,devCount,getBaseContext(),mHandler);
                        break;
                    case 2:
                        cgm=new RemoteMQTTDevice(name,devCount,getBaseContext(),mHandler);
                        break;
                    case 3:
                        cgm=new FakeDevice(name,devCount,getBaseContext(),mHandler);
                        break;
                    default:
                        Log.e(TAG,"Unknown CGM type: "+type);
                        break;
                }
                if (cgm!=null) {
                    cgms.add(cgm);
                    devCount+=1;
                } else {
                    Log.d(TAG,"No device added. Should not see this message");
                }
            }
        }
        Log.i(TAG,"Added "+(devCount-1)+" devices to the device list");
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0);
        notificationBuilder = new Notification.Builder(getApplicationContext())
                .setContentTitle(getText(R.string.cgm_service_title))
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.sandclock)
                .setLargeIcon(bm);
//        Intent uiIntent = new Intent("com.ktind.cgm.SERVICE_READY");
//        sendBroadcast(uiIntent);
    }

    public AbstractDevice findDevice(String deviceID){
        for (AbstractDevice cgm:cgms){
            String devIDStr="device_"+String.valueOf(cgm.getDeviceID());
            if (deviceID.equals(devIDStr)){
                return cgm;
            }
        }
        return null;
    }

    public long getDeviceNextReading(String deviceID){
        AbstractDevice cgm=findDevice(deviceID);
        return System.currentTimeMillis()-cgm.getNextReadingTime().getTime();
    }

    public String getDeviceName(String deviceID){
        AbstractDevice cgm=findDevice(deviceID);
        return cgm.getName();
    }

    public long getPollInterval(String deviceID){
        AbstractDevice cgm=findDevice(deviceID);
        return cgm.getPollInterval();
    }

    public ArrayList<AbstractDevice> getDevices(){
        return cgms;
    }

    public int getLastBG(String deviceID) throws NoDataException {
        AbstractDevice cgm=findDevice(deviceID);
        return cgm.getLastDownloadObject().getLastReading();
    }

    public Trend getLastTrend(String deviceID) throws NoDataException {
        AbstractDevice cgm=findDevice(deviceID);
        return cgm.getLastDownloadObject().getLastTrend();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification=notificationBuilder.build();
        for (final AbstractDevice cgm:cgms) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    cgm.start();
                }
            },"CGM"+cgm.deviceIDStr).start();
        }
        startForeground(0,notification);
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    public void helloWorld(){
        Log.d(TAG,"Hello world");
    }

    public ArrayList<DownloadObject> getDownloadData(){
        ArrayList<DownloadObject> results=new ArrayList<DownloadObject>(cgms.size());
        for (AbstractDevice cgm:cgms){
            try {
                results.add(cgm.getLastDownloadObject());
            } catch (NoDataException e) {
                Log.e(TAG,"No last download");
                e.printStackTrace();
            }
        }
        return results;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestory called");
        stopForeground(true);
        for (AbstractDevice cgm:cgms){
            cgm.stop();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        public DeviceDownloadService getServerInstance() {
            return DeviceDownloadService.this;
        }
    }
}