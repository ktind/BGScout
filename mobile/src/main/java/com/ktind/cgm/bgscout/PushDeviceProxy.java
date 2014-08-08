package com.ktind.cgm.bgscout;

import android.os.AsyncTask;
import android.os.Handler;

/**
 * Created by klee24 on 8/7/14.
 */
public class PushDeviceProxy extends AsyncTask<Void,Void,DeviceDownloadObject> implements DeviceInterface {
    AbstractPushDevice cgmDevice;
    Handler mHandler=new Handler();

    public PushDeviceProxy(AbstractPushDevice abstractPushDevice) {
        setCgmDevice(abstractPushDevice);
    }

    public AbstractPushDevice getCgmDevice() {
        return cgmDevice;
    }

    public void setCgmDevice(AbstractPushDevice cgmDevice) {
        this.cgmDevice = cgmDevice;
    }

    @Override
    protected DeviceDownloadObject doInBackground(Void... params) {
        cgmDevice.start();
        try {
            return cgmDevice.getLastDownloadObject();
        } catch (AbstractDevice.NoDownloadException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void onPostExecute(DeviceDownloadObject deviceDownloadObject) {
        super.onPostExecute(deviceDownloadObject);
        this.fireMonitors();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void fireMonitors() {

    }

    @Override
    public void stopMonitors() {

    }
}
