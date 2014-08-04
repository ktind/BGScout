package com.ktind.cgm.bgscout;

import android.os.AsyncTask;
import android.os.Handler;

/**
 * Created by klee24 on 8/3/14.
 */
public class DeviceProxy extends AsyncTask<AbstractCGMDevice,Void,Void> {
    AbstractCGMDevice cgmDevice;
    Handler mHandler;

    DeviceProxy(AbstractCGMDevice cgm,Handler mH){
        this.cgmDevice=cgm;
        this.mHandler=mH;
    }

    @Override
    protected Void doInBackground(AbstractCGMDevice... devices) {
        cgmDevice.download();
//        cgmDevice.doDownload();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        cgmDevice.fireMonitors();
    }
//    public Runnable pollCallback = new Runnable() {
//        @Override
//        public void run() {
//            long nextFire=45000;
//            for (AbstractCGMDevice cgm:cgms){
//                long tmpNextFire=cgm.nextFire();
//                if (nextFire<tmpNextFire) {
//                    Log.d(TAG,"Setting nextFire to: "+tmpNextFire);
//                    nextFire = tmpNextFire;
//                }
//            }
//            Log.d(TAG,"Calculated nextFire to: "+nextFire);
//            mHandler.postDelayed(pollDevices,nextFire);
//        }
//    };
}
