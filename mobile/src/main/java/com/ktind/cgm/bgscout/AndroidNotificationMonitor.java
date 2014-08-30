package com.ktind.cgm.bgscout;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import java.util.ArrayList;
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

 * TODO This is a HORRIBLE class. Needs to be refactored majorly - Completely violates DRY
 * FIXME most of this needs to be passed in from the device itself as a message - that way the logic stays with the device and can be easily passed to the UI
 */
public class AndroidNotificationMonitor extends AbstractMonitor {
    private static final String TAG = AndroidNotificationMonitor.class.getSimpleName();
    protected Notification.Builder notifBuilder;
    protected NotificationManager mNotifyMgr;
    final protected String monitorType="android notification";
    protected boolean isSilenced=false;
    protected Date timeSilenced;
    protected AlarmReceiver alarmReceiver=new AlarmReceiver();
    protected DownloadObject lastDownload;
    protected ArrayList<DownloadObject> previousDownloads=new ArrayList<DownloadObject>();
    protected final int MAXPREVIOUS=3;
    private PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
    private Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon);
    private final int SNOOZEDURATION=1800000;
    private SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    // Good is defined as one that has all data that we need to convey our message
    private DownloadObject lastKnownGood;
    private final int MINUPLOADERBATTERY=40;
    private final int MINDEVICEBATTERY=20;



    public void setNotifBuilder(Notification.Builder notifBuilder) {
        this.notifBuilder = notifBuilder;
    }

    AndroidNotificationMonitor(String name,int devID,Context contxt){
        super(name, devID, contxt, "android_notification");
        init();
    }

    public void init(){
        mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon);
        this.setNotifBuilder(new Notification.Builder(context)
                .setContentTitle(name)
                .setContentText("Monitor started. No data yet")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setSmallIcon(R.drawable.sandclock)
                .setLargeIcon(bm));
        Notification notification = notifBuilder.build();
        mNotifyMgr.notify(deviceID, notification);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        this.setAllowVirtual(true);
//        alarmReceiver=new AlarmReceiver();
        context.registerReceiver(alarmReceiver, new IntentFilter(Constants.SNOOZE_INTENT));
    }

    @Override
    public void doProcess(DownloadObject dl) {
        if (previousDownloads!=null) {
            if (previousDownloads.size() > 0 && previousDownloads.get(previousDownloads.size() - 1).equals(dl)) {
                Log.i(TAG, "Received a duplicate reading. Ignoring it");
                return;
            } else {
                Log.d(TAG,"Download determined to be a new reading");
            }
            previousDownloads.add(dl);
            if (previousDownloads.size()>MAXPREVIOUS)
                previousDownloads.remove(0);
            Log.d(TAG,"Previous download size: "+previousDownloads.size());
        } else {
            Log.w(TAG, "No previous downloads?");
        }
        if (dl.getEgvRecords().length>0)
            lastKnownGood = dl;

        // TODO add devicetype to the download object so that we can instantiate the proper analyzer
        AbstractDownloadAnalyzer downloadAnalyzer=new G4DownloadAnalyzer(dl, context);
        AnalyzedDownload analyzedDownload=downloadAnalyzer.analyze();

        if (isSilenced){
            long duration=new Date().getTime()-timeSilenced.getTime();
            // Snooze for 30 minutes at a time
            if (duration>SNOOZEDURATION) {
                Log.v(TAG,"Resetting snooze timer for "+deviceIDStr);
                isSilenced = false;
            }
            Log.v(TAG,"Alarm "+getName()+"("+deviceIDStr+"/"+monitorType+") is snoozed");
        }
        for (Conditions condition:analyzedDownload.getConditions()){
            Log.v(TAG,"Condition: "+condition);
        }

        mNotifyMgr.notify(deviceID, buildNotification(analyzedDownload));
        try {
            savelastSuccessDate(dl.getLastRecordReadingDate().getTime());
        } catch (NoDataException e) {
            Log.d(TAG,"No data in reading to get the last date");
        }
    }

    private Notification buildNotification(AnalyzedDownload dl){
        setDefaults();
        setSound(dl);
        setTicker(dl);
        setActions(dl);
        setContent(dl);
        setIcon(dl);

        return notifBuilder.build();
    }
    protected void setSound(AnalyzedDownload dl){
        ArrayList<Conditions> conditions=dl.getConditions();
        Uri uri = Uri.EMPTY;
        // allows us to give some sounds higher precedence than others
        // I'm thinking I'll need to set a priority to the enums to break ties but this should work for now
        // If the loop isn't broken then the last condition in the queue wins
        boolean breakloop=false;
        for (Conditions condition:conditions) {
            switch (condition) {
                case CRITICALHIGH:
                    uri = Uri.parse(sharedPref.getString(deviceIDStr + "_critical_high_ringtone", "DEFAULT_SOUND"));
                    breakloop=true;
                    break;
                case WARNHIGH:
                    uri = Uri.parse(sharedPref.getString(deviceIDStr + "_high_ringtone", "DEFAULT_SOUND"));
                    breakloop=true;
                    break;
                case INRANGE:
                    break;
                case WARNLOW:
                    uri = Uri.parse(sharedPref.getString(deviceIDStr + "_low_ringtone", "DEFAULT_SOUND"));
                    breakloop=true;
                    break;
                case CRITICALLOW:
                    uri=Uri.parse(sharedPref.getString(deviceIDStr + "_critical_low_ringtone", "DEFAULT_SOUND"));
                    breakloop=true;
                    break;
                case DOWNLOADFAILED:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case DEVICEDISCONNECTED:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case NODATA:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case STALEDATA:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case UPLOADERCRITICALLOW:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case UPLOADERLOW:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case DEVICECRITICALLOW:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case DEVICELOW:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case DEVICEMSGS:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                case UNKNOWN:
                    uri=Settings.System.DEFAULT_NOTIFICATION_URI;
                    break;
                default:
                    break;
            }
            if (breakloop)
                break;
        }
        notifBuilder.setSound(uri);

    }
    
    public void setTicker(AnalyzedDownload dl){
        ArrayList<Conditions> conditions=dl.getConditions();
        String message="";
        for (Conditions condition:conditions) {
            try {
                if (condition == Conditions.CRITICALHIGH || condition == Conditions.WARNHIGH ||
                        condition == Conditions.INRANGE || condition == Conditions.WARNLOW ||
                        condition == Conditions.CRITICALLOW) {
                    if (!message.equals(""))
                        message += "\n";
                    message += dl.getLastReading() + " " + dl.getUnit() + " " + dl.getLastTrend().toString();
                }
                switch (condition) {
                    case DOWNLOADFAILED:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Download failed";
                        break;
                    case DEVICEDISCONNECTED:
                        if (!message.equals(""))
                            message += "\n";
                        message += "CGM appears to be disconnected";
                        break;
                    case NODATA:
                        if (!message.equals(""))
                            message += "\n";
                        message += "No data available in download";
                        break;
                    case STALEDATA:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Data in download is over " + ((new Date().getTime() - dl.getLastRecordReadingDate().getTime()) / 1000) / 60;
                        break;
                    case UPLOADERCRITICALLOW:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Uploader is critically low: " + dl.getUploaderBattery();
                        break;
                    case UPLOADERLOW:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Uploader is low: " + dl.getUploaderBattery();
                        break;
                    case DEVICECRITICALLOW:
                        if (!message.equals(""))
                            message += "\n";
                        message += "CGM is critically low: " + dl.getUploaderBattery();
                        break;
                    case DEVICELOW:
                        if (!message.equals(""))
                            message += "\n";
                        message += "CGM is low: " + dl.getUploaderBattery();
                        break;
                    case DEVICEMSGS:
                        break;
                    case UNKNOWN:
                        if (!message.equals(""))
                            message += "\n";
                        message += "Unidentified condition";
                        break;
                    case REMOTEDISCONNECTED:
                        if (!message.equals(""))
                            message+="\n";
                        message+=Conditions.REMOTEDISCONNECTED.toString();
                        break;
                    case NONE:
                        break;
                }
            } catch (NoDataException e) {
                if (!message.equals(""))
                    message += "\n";
                message += "No data available in download";
            }
            notifBuilder.setTicker(message);
        }
        
    }

    private void setDefaults(){
        this.notifBuilder=new Notification.Builder(context)
                .setContentTitle(name)
                .setContentText("Default text")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setSmallIcon(R.drawable.sandclock)
                .setLargeIcon(bm);
    }

    protected void setActions(AnalyzedDownload dl){
        ArrayList<Conditions> conditions=dl.getConditions();
        for (Conditions condition:conditions){
            if (!isSilenced) {
                if (condition == Conditions.CRITICALHIGH
                        || condition == Conditions.WARNHIGH
                        || condition == Conditions.WARNLOW
                        || condition == Conditions.CRITICALLOW) {
                    Intent snoozeIntent = new Intent(Constants.SNOOZE_INTENT);
                    snoozeIntent.putExtra("device", deviceIDStr);
                    PendingIntent snoozePendIntent = PendingIntent.getBroadcast(context, deviceID, snoozeIntent, 0);
                    // TODO make the snooze time configurable
                    String snoozeActionText="Snooze for "+(SNOOZEDURATION/1000)/60+" minutes";
                    notifBuilder.addAction(android.R.drawable.ic_popup_reminder, snoozeActionText, snoozePendIntent);
                }
            }

        }
    }

    protected void setContent(AnalyzedDownload dl){
        String msg="";
        for (AlertMessage message:dl.getMessages()){
            if (!msg.equals(""))
                msg+="\n";
            msg+=message.getMessage();
        }
        notifBuilder.setStyle(new Notification.BigTextStyle().bigText(msg))
                .setContentText(msg);
    }

    protected void setIcon(AnalyzedDownload dl){
        int iconLevel=60;
        int state=0;
        int range=0;

        ArrayList<Conditions> conditions=dl.getConditions();
        if (conditions.contains(Conditions.DEVICELOW) ||
                conditions.contains(Conditions.DEVICECRITICALLOW) ||
                conditions.contains(Conditions.UPLOADERLOW) ||
                conditions.contains(Conditions.UPLOADERCRITICALLOW) ||
                conditions.contains(Conditions.DOWNLOADFAILED) ||
                conditions.contains(Conditions.DEVICEDISCONNECTED) ||
                conditions.contains(Conditions.NODATA) ||
                conditions.contains(Conditions.STALEDATA) ||
                conditions.contains(Conditions.UNKNOWN) ||
                conditions.contains(Conditions.REMOTEDISCONNECTED)){
            state=1;
        }
        if (conditions.contains(Conditions.CRITICALHIGH) ||
                conditions.contains(Conditions.WARNHIGH)){
            range=1;
        }
        if (conditions.contains(Conditions.CRITICALLOW) ||
                conditions.contains(Conditions.WARNLOW)){
            range=2;
        }
        try {
            Trend trend = dl.getLastTrend();
            iconLevel = trend.getVal() + (state * 10) + (range * 20);
        } catch (NoDataException e) {
            iconLevel=60;
//            e.printStackTrace();
        }
        notifBuilder.setSmallIcon(R.drawable.smicons, iconLevel);
    }

    @Override
    public void start() {
        super.start();
        init();
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping monitor " + monitorType + " for " + name);
        mNotifyMgr.cancel(deviceID);
        if (context != null && alarmReceiver != null)
            context.unregisterReceiver(alarmReceiver);

    }

    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Constants.SNOOZE_INTENT)){
                if (intent.getExtras().get("device").equals(deviceIDStr)) {
                    Log.d(TAG, deviceIDStr + ": Received a request to snooze alarm on " + intent.getExtras().get("device"));
                    // Only capture the first snooze operation.. ignore others until it is reset
                    if (!isSilenced) {
                        isSilenced = true;
                        timeSilenced = new Date();
                    }
                    if (lastDownload!=null)
                        doProcess(lastDownload);
                }else{
                    Log.d(TAG,deviceIDStr+": Ignored a request to snooze alarm on "+intent.getExtras().get("device"));
                }
            }
        }
    }
}