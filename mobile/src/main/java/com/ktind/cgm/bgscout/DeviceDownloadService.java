package com.ktind.cgm.bgscout;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;

public class DeviceDownloadService extends Service {
    private static final String TAG = DeviceDownloadService.class.getSimpleName();
    private ArrayList<AbstractDevice> cgms=new ArrayList<AbstractDevice>();
    private DeviceDownloadObject lastDownload;
    private Notification.Builder notificationBuilder;
    private Handler mHandler=new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        CGMBus.getInstance().register(this);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
//        cgms.add(new RemoteMQTTDevice("Remote Device",5,getBaseContext(),mHandler));
        String[] device_list={"device_1","device_2","device_3","device_4"};
        int devCount=1;
        for (String dev:device_list) {
            boolean enabled = sharedPref.getBoolean(dev+"_enable", false);
            if (enabled){
                String name = sharedPref.getString(dev+"_name","");
                // FIXME - not sure whats going on here? Why do I have to cast this from a string? Was it setup in the XML wrong?
                int type = Integer.valueOf(sharedPref.getString(dev+"_type","0"));
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
                    default:
                        Log.e(TAG,"Unknown CGM type: "+type);
                        break;
                }
                if (cgm!=null) {
                    cgms.add(cgm);
                    Log.d(TAG,"Adding CGM to list of cgms");
                    devCount+=1;
                } else {
                    Log.d(TAG,"No device added. Should not see this message");
                }
            }
        }
        Log.i(TAG,"Added "+devCount+" devices to the device list");
//        cgms.add(new G4CGMDevice("Melissa",1,getBaseContext(),mHandler));
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0);
        notificationBuilder = new Notification.Builder(getApplicationContext())
                .setContentTitle(getText(R.string.cgm_service_title))
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.icon24x24)
                .setNumber(cgms.size())
                .setLargeIcon(bm);
    }

//    private Runnable pollDevices = new Runnable() {
//        @Override
//        public void run() {
//            for (AbstractPollDevice cgm:cgms) {
//                innerPollDevice deviceProxy=new innerPollDevice(cgm);
//                deviceProxy.execute();
//            }
////            mHandler.postDelayed(pollDevices, nextFire);
//        }
//    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification=notificationBuilder.build();
        for (AbstractDevice cgm:cgms) {
            cgm.start();
//            innerPollDevice deviceProxy=new innerPollDevice(cgm);
//            deviceProxy.execute();
        }
//        mHandler.post(pollDevices);
        startForeground(1,notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        CGMBus.getInstance().unregister(this);
        stopForeground(false);
        for (AbstractDevice cgm:cgms){
            cgm.stop();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Subscribe
    public void onDeviceDownloadObject(AbstractPollDevice cgm) {
        Log.d(TAG, "message received from thread!");
    }

//    public class innerPollDevice extends AsyncTask<Void,Void,DeviceDownloadObject> {
//        AbstractPollDevice cgmDevice;
//        Handler myInnerHandler=new Handler();
//
//        innerPollDevice(AbstractPollDevice cD){
//            super();
//            this.cgmDevice=cD;
//        }
//
//        public Runnable nextPoll = new Runnable() {
//            @Override
//            public void run() {
//                CGMBus.getInstance().post(cgmDevice);
////                onDeviceDownloadObject(cgmDevice);
//            }
//        };
//
//        @Override protected DeviceDownloadObject doInBackground(Void... params) {
//            return cgmDevice.start();
//        }
//
//        @Override protected void onPostExecute(DeviceDownloadObject result) {
//            super.onPostExecute(result);
//            result.getDevice().fireMonitors();
//            myInnerHandler.postDelayed(nextPoll, result.getDevice().nextFire());
//        }
//
//    }

}