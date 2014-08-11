package com.ktind.cgm.bgscout;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
//TODO Otto is no longer used.. remove it?

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
                int type = Integer.valueOf(sharedPref.getString(dev + "_type", "0"));
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
        //FIXME devCount is really a device ID counter - should probably start with 0 to avoid confusion, no?
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

    public int getLastBG(String deviceID) throws AbstractDevice.NoDownloadException {
        AbstractDevice cgm=findDevice(deviceID);
        return cgm.getLastBG();
    }

    public Trend getLastTrend(String deviceID) throws AbstractDevice.NoDownloadException {
        AbstractDevice cgm=findDevice(deviceID);
        return cgm.getLastTrend();
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

        return super.onStartCommand(intent, flags, startId);
    }

    public ArrayList<DeviceDownloadObject> getData(){
        ArrayList<DeviceDownloadObject> results=new ArrayList<DeviceDownloadObject>(cgms.size());
        for (AbstractDevice cgm:cgms){
            try {
                results.add(cgm.getLastDownloadObject());
            } catch (AbstractDevice.NoDownloadException e) {
                e.printStackTrace();
            }
        }
        return results;
    }

    @Override
    public void onDestroy() {
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