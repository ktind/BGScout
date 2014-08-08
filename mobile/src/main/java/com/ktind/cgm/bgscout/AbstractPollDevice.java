package com.ktind.cgm.bgscout;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.Date;

/**
 * Created by klee24 on 8/2/14.
 */
abstract public class AbstractPollDevice extends AbstractDevice {
    private static final String TAG = AbstractPollDevice.class.getSimpleName();
    protected long nextFire=45000L;
    protected int pollInterval=302000;
    // Should this be final?
    protected DeviceProxy deviceProxy;


    public AbstractPollDevice(String n, int deviceID, Context appContext, Handler mH){
        super(n,deviceID,appContext,mH);
        deviceProxy=new DeviceProxy(this,mH);
    }

    public DeviceProxy getDeviceProxy() {
        return deviceProxy;
    }

    abstract protected DeviceDownloadObject doDownload();

    final public void start(){
        getDeviceProxy().execute();
    }

    public int getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(int pollInterval) {
        Log.d(TAG,"Setting poll interval to: "+pollInterval);
        this.pollInterval = pollInterval;
    }

    @Override
    public void stop() {
        super.stop();
        deviceProxy.stopPolling();
    }

    public long nextFire(){
        return nextFire(getPollInterval());
    }

    public long nextFire(long millis){
        try {
            long diff=(millis-(new Date().getTime() - getLastDownloadObject().getEgvRecords()[getLastDownloadObject().getEgvRecords().length-1].getDate().getTime()));
            Log.d(TAG,"nextFire calculated to be: "+diff+" for "+getName()+" using a poll interval of "+millis);
            if (diff<0) {
                Log.w(TAG,"nextFire returning 45 seconds because diff was negative");
                return 45000;
            }
            return diff;
        } catch (NoDownloadException e){
            Log.d(TAG,"nextFire returning 45 seconds because there wasn't a lastdownloadobject set");
            return 450000;
        }
    }
}
