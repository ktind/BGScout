package com.ktind.cgm.bgscout;

import android.os.AsyncTask;
import android.os.Handler;

/**
 * Created by klee24 on 8/3/14.
 */
public class DeviceProxy extends AsyncTask<AbstractPollDevice,Void,Void> {
    AbstractPollDevice cgmDevice;
    Handler mHandler;

    DeviceProxy(AbstractPollDevice cgm,Handler mH){
        this.cgmDevice=cgm;
        this.mHandler=mH;
    }

    public Runnable pollDevice = new Runnable() {
        @Override
        public void run() {
            new DeviceProxy(cgmDevice,mHandler).execute();
        }
    };

    @Override
    protected Void doInBackground(AbstractPollDevice... devices) {
        cgmDevice.doDownload();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        cgmDevice.fireMonitors();
        mHandler.postDelayed(pollDevice,cgmDevice.nextFire());
    }

    public void stopPolling(){
        mHandler.removeCallbacks(pollDevice);
    }
}
