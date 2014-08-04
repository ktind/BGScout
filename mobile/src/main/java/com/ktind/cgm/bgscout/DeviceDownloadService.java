package com.ktind.cgm.bgscout;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;

public class DeviceDownloadService extends Service {
    private static final String TAG = DeviceDownloadService.class.getSimpleName();
    private ArrayList<AbstractCGMDevice> cgms=new ArrayList<AbstractCGMDevice>();
    private DeviceDownloadObject lastDownload;
    private Notification.Builder notificationBuilder;
    private Handler mHandler=new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
        CGMBus.getInstance().register(this);
//        cgms.add(new G4CGMDevice("Melissa",1,getBaseContext(),mHandler));
        cgms.add(new RemoteMongoDevice("Melissa",1,getBaseContext(),mHandler));
//        cgms.add(new FakeCGMDevice("Billy",2,this.getBaseContext()));
//        cgms.add(new FakeCGMDevice("Sue",3,this.getBaseContext()));
        Bitmap bm = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0);
        notificationBuilder = new Notification.Builder(getApplicationContext())
                .setContentTitle(getText(R.string.cgm_service_title))
                .setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.icon24x24)
                .setNumber(cgms.size())
                .setLargeIcon(bm);
    }

    //FIXME right now this treats all CGMs as firing at the same time. Need individual timers.
    private Runnable pollDevices = new Runnable() {
        @Override
        public void run() {
            for (AbstractCGMDevice cgm:cgms) {
                innerDeviceProxy deviceProxy=new innerDeviceProxy(cgm);
                deviceProxy.execute();
            }
//            mHandler.postDelayed(pollDevices, nextFire);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification=notificationBuilder.build();
        for (AbstractCGMDevice cgm:cgms) {
            innerDeviceProxy deviceProxy=new innerDeviceProxy(cgm);
            deviceProxy.execute();
        }
//        mHandler.post(pollDevices);
        startForeground(1,notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
//        mHandler.removeCallbacks(pollDevices);
        CGMBus.getInstance().unregister(this);
        stopForeground(false);
        for (AbstractCGMDevice cgm:cgms){
            cgm.stopMonitors();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Subscribe
    public void onDeviceDownloadObject(AbstractCGMDevice cgm) {
        Log.d(TAG,"message received! Starting over again!");
        innerDeviceProxy deviceProxy=new innerDeviceProxy(cgm);
        deviceProxy.execute();
    }

    public class innerDeviceProxy extends AsyncTask<Void,Void,DeviceDownloadObject> {
        AbstractCGMDevice cgmDevice;
        Handler myInnerHandler=new Handler();

        innerDeviceProxy(AbstractCGMDevice cD){
            super();
            this.cgmDevice=cD;
        }

        public Runnable nextPoll = new Runnable() {
            @Override
            public void run() {
                CGMBus.getInstance().post(cgmDevice);
//                onDeviceDownloadObject(cgmDevice);
            }
        };

        @Override protected DeviceDownloadObject doInBackground(Void... params) {
            return cgmDevice.download();
        }

        @Override protected void onPostExecute(DeviceDownloadObject result) {
            super.onPostExecute(result);
            result.getDevice().fireMonitors();
            myInnerHandler.postDelayed(nextPoll, result.getDevice().nextFire());
        }

    }

}