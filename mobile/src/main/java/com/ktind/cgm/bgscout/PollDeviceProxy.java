package com.ktind.cgm.bgscout;

import android.os.AsyncTask;
import android.os.Handler;

/**
 * Created by klee24 on 8/7/14.
 */
public class PollDeviceProxy extends AsyncTask<Void,Void,DeviceDownloadObject> implements DeviceInterface {
    AbstractPollDevice cgmDevice;
    Handler mHandler=new Handler();

    PollDeviceProxy(AbstractPollDevice cgmDevice){
        super();
        this.cgmDevice=cgmDevice;
    }

    public Runnable nextPoll = new Runnable() {
        @Override
        public void run() {
            start();
//            CGMBus.getInstance().post(cgmDevice);
        }
    };

    @Override protected DeviceDownloadObject doInBackground(Void... params) {
        cgmDevice.start();
        try {
            return cgmDevice.getLastDownloadObject();
        } catch (AbstractDevice.NoDownloadException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override protected void onPostExecute(DeviceDownloadObject result) {
        super.onPostExecute(result);
        this.fireMonitors();
        mHandler.postDelayed(nextPoll, cgmDevice.nextFire());
    }

    @Override
    public void start() {
        execute();
//        try {
//            return cgmDevice.getLastDownloadObject();
//        } catch (AbstractDevice.NoDownloadException e) {
//            // TODO maybe add a new exception saying there was no start? I can't return a null object here - it could lead to more NPEs?
//            e.printStackTrace();
//            return null;
//        }
    }

    @Override
    public void stop() {

    }

    @Override
    public void fireMonitors() {
        cgmDevice.fireMonitors();
    }

    @Override
    public void stopMonitors() {
        cgmDevice.stopMonitors();
    }
}
