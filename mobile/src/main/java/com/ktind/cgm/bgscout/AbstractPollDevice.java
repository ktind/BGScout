package com.ktind.cgm.bgscout;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import java.util.Date;

/**
 * Created by klee24 on 8/2/14.
 */
abstract public class AbstractPollDevice extends AbstractDevice {
    private static final String TAG = AbstractPollDevice.class.getSimpleName();
    protected long nextFire=45000L;
    AlarmReceiver alarmReceiver;


    public AbstractPollDevice(String n, int deviceID, Context appContext, Handler mH){
        super(n,deviceID,appContext,mH);
    }

    abstract protected DownloadObject doDownload();

    // entry point
    final public void start(){
        super.start();
        AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "G4DL");
        wl.acquire();
        Log.i(TAG,"Performing initial download");
        download();
        Log.i(TAG,"Initial download complete");
        wl.release();
        Log.d(TAG,"Next readingTime to be: "+getNextReadingTime().toString()+" Current time: "+new Date());
        Intent intent = new Intent("com.ktind.cgm.DEVICE_POLL");
        intent.putExtra("device",deviceIDStr);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(appContext, deviceID, intent, 0);
        alarmMgr.set(AlarmManager.RTC_WAKEUP,getNextReadingTime().getTime(),alarmIntent);
        alarmReceiver=new AlarmReceiver();
        appContext.registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.DEVICE_POLL"));
    }

    public Date getNextReadingTime() {
        long msSinceLastReading;
        long multiplier;
        long timeForNextReading;
        try {
            msSinceLastReading = new Date().getTime() - getLastDownloadObject().getLastReadingDate().getTime();
            if (msSinceLastReading > getPollInterval()) {
                Log.w(TAG, "Possible missed readings?");
                Log.d(TAG, "Last Date: " + getLastDownloadObject().getLastReadingDate() + " Current: " + new Date());
            }
            multiplier = (msSinceLastReading / getPollInterval())+1;
            timeForNextReading = ((multiplier * getPollInterval()) - msSinceLastReading)+System.currentTimeMillis();
            if (timeForNextReading<0){
                Log.w(TAG,"Should not see this. Something is wrong with my math");
                timeForNextReading=getPollInterval();
            }
            return new Date(timeForNextReading);
        } catch (DeviceException e) {
            return new Date(System.currentTimeMillis()+45000L);
        }
    }

    public void pollDevice(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "G4DL");
                    wl.acquire();
                    download();
                    wl.release();
                }
            },deviceIDStr+"-thread").start();
    }

    public void download(){
        Log.i(TAG,"Beginning download");
        stats.startDownloadTimer();
        doDownload();
        stats.stopDownloadTimer();
        Log.i(TAG,"Download complete");
        onDownload();
    }

    public void setPollInterval(int pollInterval) {
        Log.d(TAG,"Setting poll interval to: "+pollInterval);
        this.pollInterval = pollInterval;
    }

    @Override
    public void stop() {
        super.stop();
        if (alarmReceiver!=null)
            appContext.unregisterReceiver(alarmReceiver);
    }

    public long nextFire(){
        return nextFire(getPollInterval());
    }

    public long nextFire(long millis){
        try {
            long lastDLlong=getLastDownloadObject().getEgvRecords()[getLastDownloadObject().getEgvRecords().length-1].getDate().getTime();
            Log.d(TAG,"nextFire calculated last dl to be: "+lastDLlong + " currentMillis: "+System.currentTimeMillis());
            long diff=(millis-(System.currentTimeMillis() - lastDLlong));
            Log.d(TAG,"nextFire calculated to be: "+diff+" for "+getName()+" using a poll interval of "+millis);
            if (diff<0) {
                Log.w(TAG,"nextFire returning 45 seconds because diff was negative");
                return millis;
            }
            return diff;
        } catch (DeviceException e) {
            Log.d(TAG,"nextFire returning "+millis+" seconds because there wasn't a lastdownloadobject set");
            return millis;
        }
    }

    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.ktind.cgm.DEVICE_POLL")){
                if (intent.getExtras().get("device").equals(deviceIDStr)) {
                    AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
                    Log.d(TAG, deviceIDStr+": Received a request to poll " + intent.getExtras().get("device"));
                    pollDevice();
                    Log.d(TAG,"Next readingTime to be: "+getNextReadingTime().toString()+" Current time: "+new Date());
                    Intent pIntent = new Intent("com.ktind.cgm.DEVICE_POLL");
                    pIntent.putExtra("device",deviceIDStr);
                    PendingIntent alarmIntent = PendingIntent.getBroadcast(appContext, deviceID, pIntent, 0);
                    alarmMgr.set(AlarmManager.RTC_WAKEUP,getNextReadingTime().getTime(),alarmIntent);
                } else {
                    Log.d(TAG,deviceIDStr+": Ignored a request for "+intent.getExtras().get("device")+" to perform an Device Poll operation");
                }
            }
        }
    }
}