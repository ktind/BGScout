package com.ktind.cgm.bgscout;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

/**
 * Created by klee24 on 8/3/14.
 */
public class DeviceProxy extends AsyncTask<AbstractPollDevice,Void,Void> {
    private static final String TAG = DeviceProxy.class.getSimpleName();
    AbstractPollDevice cgmDevice;
    Handler mHandler;
    private AsyncTask mTask;

    DeviceProxy(AbstractPollDevice cgm,Handler mH){
        this.cgmDevice=cgm;
        this.mHandler=mH;
    }

//    //FIXME Unable to stop this and it seems to run multiple times with each service start.
//    public Runnable pollDevice = new Runnable() {
//        @Override
//        public void run() {
//            mTask=new DeviceProxy(cgmDevice,mHandler).execute();
//        }
//    };

    @Override
    protected Void doInBackground(AbstractPollDevice... devices) {
        cgmDevice.download();
        cgmDevice.fireMonitors();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d(TAG,"DeviceProxy thread ended for "+cgmDevice.getName()+"("+cgmDevice.getDeviceType()+")");
    }

    public void stopPolling(){
        Log.i(TAG,"Will no longer poll "+cgmDevice.getName()+"("+cgmDevice.getDeviceType()+")");
//        mHandler.removeCallbacks(pollDevice);
//        this.cancel(true);
//        cgmDevice.stopMonitors();
    }
}
