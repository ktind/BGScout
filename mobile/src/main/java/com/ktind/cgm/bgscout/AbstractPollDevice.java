package com.ktind.cgm.bgscout;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.Date;

/**
 Copyright (c) 2014, Kevin Lee (klee24@gmail.com)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice, this
 list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
abstract public class AbstractPollDevice extends AbstractDevice {
    private static final String TAG = AbstractPollDevice.class.getSimpleName();
    protected long nextFire=Constants.DEFAULTRETRYINTERVAL;
    AlarmReceiver alarmReceiver;
    AlarmManager alarmMgr;
    PendingIntent alarmIntent;


    public AbstractPollDevice(String n, int deviceID, Context context, String driver){
        super(n,deviceID,context,driver);
    }

    abstract protected DownloadObject doDownload();

    // entry point
    public void start(){
        super.start();
        if (state==State.STARTED)
            return;
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "G4DLInit");
        wl.acquire();
        Log.i(TAG,"Performing initial download");
        download();
        Log.i(TAG,"Initial download complete");
        wl.release();
        Log.d(TAG,"Next readingTime to be: "+getNextReadingTime().toString()+" Current time: "+new Date());
        Intent intent = new Intent(Constants.DEVICE_POLL);
        intent.putExtra("device",deviceIDStr);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, deviceID, intent, 0);
        alarmMgr.set(AlarmManager.RTC_WAKEUP,getNextReadingTime().getTime(),alarmIntent);
        alarmReceiver=new AlarmReceiver();
        context.registerReceiver(alarmReceiver, new IntentFilter(Constants.DEVICE_POLL));
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
            return new Date(timeForNextReading+Constants.READINGDELAY);
        } catch (DeviceException e) {
            return new Date(System.currentTimeMillis()+Constants.DEFAULTRETRYINTERVAL);
        }
    }

    public void pollDevice(){
            new Thread(new Runnable() {
                @Override
                public void run() {
                    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                    PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "G4DL");
                    wl.acquire();
                    download();
                    wl.release();
                }
            },deviceIDStr+"-thread").start();
    }

    public void download(){
        Log.i(TAG,"Before download thread creation");
        new Thread( new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"Beginning download in download thread");
                stats.startDownloadTimer();
                Tracker tracker=((BGScout) context.getApplicationContext()).getTracker();
                long downloadTimeStart=System.currentTimeMillis();
                doDownload();
                tracker.send(new HitBuilders.TimingBuilder()
                                .setCategory("Download")
                                .setLabel(driver)
                                .setValue(System.currentTimeMillis()-downloadTimeStart)
                                .build()
                );
                stats.stopDownloadTimer();
                Log.i(TAG,"Download complete in download thread");
                onDownload();
            }
        },"Download_"+deviceIDStr).start();
        Log.i(TAG,"After download thread creation");

    }

    public void setPollInterval(int pollInterval) {
        Log.d(TAG,"Setting poll interval to: "+pollInterval);
        this.pollInterval = pollInterval;
    }

    @Override
    public void stop() {
        super.stop();
        if (state==State.STOPPED || state==State.STOPPING)
            return;
        if (alarmMgr!=null && alarmIntent!=null)
            alarmMgr.cancel(alarmIntent);
        if (alarmReceiver != null)
            context.unregisterReceiver(alarmReceiver);
    }

    public long nextFire(){
        return nextFire(getPollInterval());
    }

    public long nextFire(long millis){
        try {
            // FIXME consider using system time to determine the offset for the next reading rather than the display time to get rid of the time sync problems.
            long lastDLlong=getLastDownloadObject().getEgvRecords()[getLastDownloadObject().getEgvRecords().length-1].getDate().getTime();
            Log.d(TAG,"nextFire calculated last dl to be: "+lastDLlong + " currentMillis: "+System.currentTimeMillis());
            long diff=(millis-(System.currentTimeMillis() - lastDLlong));
            Log.d(TAG,"nextFire calculated to be: "+diff+" for "+getName()+" using a poll interval of "+millis);
            if (diff<0) {
                Log.w(TAG,"nextFire returning "+(millis/1000)+" seconds because diff was negative");
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
            if (intent.getAction().equals(Constants.DEVICE_POLL)){
                if (intent.getExtras().get("device").equals(deviceIDStr)) {
                    alarmMgr = (AlarmManager) AbstractPollDevice.this.context.getSystemService(Context.ALARM_SERVICE);
                    Log.d(TAG, deviceIDStr+": Received a request to poll " + intent.getExtras().get("device"));
                    pollDevice();
                    Log.d(TAG,"Next readingTime to be: "+getNextReadingTime().toString()+" Current time: "+new Date());
                    Intent pIntent = new Intent(Constants.DEVICE_POLL);
                    pIntent.putExtra("device",deviceIDStr);
                    alarmIntent = PendingIntent.getBroadcast(AbstractPollDevice.this.context, deviceID, pIntent, 0);
                    // FIXME - Needs to use setExact on Kitkat devices otherwise the alarm gets batched
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
                        alarmMgr.setExact(AlarmManager.RTC_WAKEUP,getNextReadingTime().getTime(),alarmIntent);
                    else
                        alarmMgr.set(AlarmManager.RTC_WAKEUP,getNextReadingTime().getTime(),alarmIntent);
                } else {
                    Log.d(TAG,deviceIDStr+": Ignored a request for "+intent.getExtras().get("device")+" to perform an Device Poll operation");
                }
            }
        }
    }
}