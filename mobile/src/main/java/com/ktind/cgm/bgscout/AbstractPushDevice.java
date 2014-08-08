package com.ktind.cgm.bgscout;

import android.content.Context;
import android.os.Handler;

/**
 * Created by klee24 on 8/7/14.
 * Incredibly small class for now but I suspect it will grow with more discoveries. Perhaps if the
 * server requests device attributes or configuration details we could pull that up here
 */
abstract public class AbstractPushDevice extends AbstractDevice {
    protected PushDeviceProxy deviceProxy;
    public AbstractPushDevice(String n, int deviceID, Context appContext, Handler mH) {
        super(n, deviceID, appContext, mH);
        deviceProxy=new PushDeviceProxy(this);
    }

    public PushDeviceProxy getDeviceProxy() {
        return deviceProxy;
    }

    abstract void onDataReady(DeviceDownloadObject ddo);

    @Override
    public void stop() {
        super.stop();
    }
}
