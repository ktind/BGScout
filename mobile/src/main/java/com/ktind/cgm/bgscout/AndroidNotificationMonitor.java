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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by klee24 on 8/2/14.
 */
public class AndroidNotificationMonitor extends AbstractMonitor {
    private static final String TAG = AndroidNotificationMonitor.class.getSimpleName();
    protected Notification.Builder notifBuilder;
    protected NotificationManager mNotifyMgr;
    final protected String monitorType="android notification";
    protected boolean isSilenced=false;
    protected Date timeSilenced;
    protected AlarmReceiver alarmReceiver;
    protected DownloadObject lastDownload;
    protected ArrayList<DownloadObject> previousDownloads=new ArrayList<DownloadObject>();
    protected final int MAXPREVIOUS=3;

    public void setNotifBuilder(Notification.Builder notifBuilder) {
        this.notifBuilder = notifBuilder;
    }

    AndroidNotificationMonitor(String name,int devID,Context appContext){
        super(name,devID,appContext);
        mNotifyMgr = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(appContext, 0, new Intent(appContext, MainActivity.class), 0);
//        Intent snoozeIntent =new Intent("com.ktind.cgm.SNOOZE_ALARM");
//        snoozeIntent.putExtra("device",deviceIDStr);
//        PendingIntent snoozePendIntent = PendingIntent.getBroadcast(appContext,deviceID,snoozeIntent,0);
        Bitmap bm = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.icon);
        this.setNotifBuilder(new Notification.Builder(appContext)
                .setContentTitle(name)
                .setContentText("Monitor started. No data yet")
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .setSmallIcon(R.drawable.sandclock)
                .setLargeIcon(bm));
        Notification notification = notifBuilder.build();
        mNotifyMgr.notify(devID, notification);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        this.setLowThreshold(Integer.valueOf(sharedPref.getString(deviceIDStr + "_low_threshold", "60")));
        this.setHighThreshold(Integer.valueOf(sharedPref.getString(deviceIDStr + "_high_threshold", "180")));
        this.setMonitorType("Android notification");
        this.setAllowVirtual(true);
        alarmReceiver=new AlarmReceiver();
        appContext.registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.SNOOZE_ALARM"));
    }

    @Override
    public void doProcess(DownloadObject dl) {
        int state=0;
        String msg="";
        try {
            if (previousDownloads!=null) {
                if (previousDownloads.size() > 0 && previousDownloads.get(previousDownloads.size() - 1).equals(dl)) {
                    Log.i(TAG, "Received a duplicate reading. Ignoring it");
                    return;
                } else {
                    Log.d(TAG,"Download determined to be a new reading");
                }
                previousDownloads.add(dl);
                if (previousDownloads.size()>=MAXPREVIOUS)
                    previousDownloads.remove(0);
                Log.d(TAG,"Previous download size: "+previousDownloads.size());
            } else {
                Log.w(TAG, "No previous downloads?");
            }

//            lastDownload=dl;
            PendingIntent contentIntent = PendingIntent.getActivity(appContext, 0, new Intent(appContext, MainActivity.class), 0);
            Bitmap bm = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.icon);
            // maybe it will help get rid of the action if we build a new notification object?
            this.setNotifBuilder(new Notification.Builder(appContext)
                    .setContentTitle(name)
                    .setContentText("Default text")
                    .setContentIntent(contentIntent)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.sandclock)
                    .setLargeIcon(bm));


            if (isSilenced){
                long duration=new Date().getTime()-timeSilenced.getTime();
                // Snooze for 30 minutes at a time
                if (duration>1800000) {
                    Log.v(TAG,"Resetting snooze timer for "+deviceIDStr);
                    isSilenced = false;
                }
                Log.v(TAG,"Alarm "+getName()+"("+deviceIDStr+"/"+monitorType+") is snoozed");
            }

            Notification notification;
//            int icon = R.drawable.questionmarkicon;
            int icon;
            notifBuilder.setSound(Uri.EMPTY);

            Log.v(TAG,"Status: "+dl.getStatus().toString());
            if (dl.getStatus() != DownloadStatus.SUCCESS && dl.getStatus() != DownloadStatus.SPECIALVALUE) {
                msg = dl.getStatus().toString();
                notifBuilder.setTicker(msg);
            }

            if (dl.getStatus() == DownloadStatus.SPECIALVALUE){
                msg = dl.getSpecialValueMessage()+"\n";
                notifBuilder.setTicker(msg);
                icon=R.drawable.exclamationmarkicon;
    //                    .setSmallIcon(R.drawable.exclamationmarkicon);
            }

            if (dl.getStatus()== DownloadStatus.NODATA){
                icon=R.drawable.sandclock;
            }

            if (new Date().getTime() - dl.getLastReadingDate().getTime() > 300000) {
                icon = R.drawable.exclamationmarkicon;
            }
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
            Log.d(TAG, "Default Notification sound: " + Settings.System.DEFAULT_NOTIFICATION_URI);
            if (dl.getLastReading() >= highThreshold) {
                String ringtoneURI = sharedPref.getString(deviceIDStr + "_high_ringtone", "DEFAULT_SOUND");
                Uri uri = Uri.parse(ringtoneURI);
                Log.d(TAG, "Notification sound: " + uri);
//                long[] vibe = {2000, 1000, 2000, 1000};
                if (!isSilenced) {
                    Intent snoozeIntent = new Intent("com.ktind.cgm.SNOOZE_ALARM");
                    snoozeIntent.putExtra("device", deviceIDStr);
                    PendingIntent snoozePendIntent = PendingIntent.getBroadcast(appContext, deviceID, snoozeIntent, 0);
                    notifBuilder.setSound(uri);
                    notifBuilder.addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendIntent);
                    //.setVibrate(vibe)
                } else {
                    Log.v(TAG, "Alarm " + getName() + " (" + getMonitorType() + ") - " + deviceIDStr + " snoozed");
                }

            }
            if (dl.getLastReading() <= lowThreshold) {
                String ringtoneURI = sharedPref.getString(deviceIDStr + "_low_ringtone", "DEFAULT_SOUND");
                Uri uri = Uri.parse(ringtoneURI);
                Log.d(TAG, "Notification sound: " + uri);
//                long[] vibe = {4000, 1000, 4000, 1000};
                if (!isSilenced) {
                    Intent snoozeIntent = new Intent("com.ktind.cgm.SNOOZE_ALARM");
                    snoozeIntent.putExtra("device", deviceIDStr);
                    PendingIntent snoozePendIntent = PendingIntent.getBroadcast(appContext, deviceID, snoozeIntent, 0);
                    notifBuilder.setSound(uri);
                    notifBuilder.addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendIntent);
                    //.setVibrate(vibe)
                } else {
                    Log.v(TAG, "Alarm " + getName() + " (" + getMonitorType() + ") - " + deviceIDStr + " snoozed");
                }
            }

            if (dl.getStatus() != DownloadStatus.SPECIALVALUE) {
//                icon = getIcon(dl.getLastReading(), dl.getLastTrend());
                msg += dl.getLastReading() + " " + dl.getUnit() + " and " + dl.getLastTrend().toString() + "\n";
                msg += new SimpleDateFormat("HH:mm:ss MM/dd").format(dl.getLastReadingDate());
            }
            icon=getIcon(dl.getLastReading(),dl.getLastTrend(),state);
            notification = notifBuilder
//                    .setSmallIcon(icon)
                    .setSmallIcon(R.drawable.smicons,icon)
                    .setStyle(new Notification.BigTextStyle().bigText(msg))
                    .setContentText(msg)
                    .build();
            mNotifyMgr.notify(deviceID, notification);
        } catch (NoDataException e) {
            Log.e(TAG,e.getMessage());
        }
    }

    private int getIcon(int bgValue,Trend trend, int state){
        // FIXME ENUM the state
//        int state=0;
        int range=0;
        if (bgValue>=highThreshold)
            range=1;
        else if (bgValue<=lowThreshold)
            range=2;
        else
            range=0;

        int iconLevel=trend.getVal()+(state*10)+(range*20);
        return iconLevel;
        // Handle "NOT COMPUTABLE", "RATE OUT OF RANGE", and anything else that crops up.
//        int icon=R.drawable.questionmarkicon;
//        if (bgValue<=lowThreshold){
//            if (trend==Trend.NONE) {
//                icon=R.drawable.nonered;
//            }else if(trend==Trend.DOUBLEUP) {
//                icon=R.drawable.arrowdoubleupred;
//            }else if(trend==Trend.SINGLEUP) {
//                icon=R.drawable.arrowupred;
//            }else if(trend==Trend.FORTYFIVEUP) {
//                icon=R.drawable.arrow45upred;
//            }else if(trend==Trend.FLAT) {
//                icon=R.drawable.arrowflatred;
//            }else if(trend==Trend.DOUBLEDOWN) {
//                icon=R.drawable.arrowdoubledownred;
//            }else if(trend==Trend.SINGLEDOWN) {
//                icon=R.drawable.arrowdownred;
//            }else if(trend==Trend.FORTYFIVEDOWN) {
//                icon=R.drawable.arrow45downred;
//            }
//        }else if (bgValue>=highThreshold){
//            if (trend==Trend.NONE) {
//                icon=R.drawable.noneyellow;
//            }else if(trend==Trend.DOUBLEUP) {
//                icon=R.drawable.arrowdoubleupyellow;
//            }else if(trend==Trend.SINGLEUP) {
//                icon=R.drawable.arrowupyellow;
//            }else if(trend==Trend.FORTYFIVEUP) {
//                icon=R.drawable.arrow45upyellow;
//            }else if(trend==Trend.FLAT) {
//                icon=R.drawable.arrowflatyellow;
//            }else if(trend==Trend.DOUBLEDOWN) {
//                icon=R.drawable.arrowdoubledownyellow;
//            }else if(trend==Trend.SINGLEDOWN) {
//                icon=R.drawable.arrowdownyellow;
//            }else if(trend==Trend.FORTYFIVEDOWN) {
//                icon=R.drawable.arrow45downyellow;
//            }
//        }else{
//            if (trend==Trend.NONE) {
//                icon=R.drawable.nonegreen;
//            }else if(trend==Trend.DOUBLEUP) {
//                icon=R.drawable.arrowdoubleupgreen;
//            }else if(trend==Trend.SINGLEUP) {
//                icon=R.drawable.arrowupgreen;
//            }else if(trend==Trend.FORTYFIVEUP) {
//                icon=R.drawable.arrow45upgreen;
//            }else if(trend==Trend.FLAT) {
//                icon=R.drawable.arrowflatgreen;
//            }else if(trend==Trend.DOUBLEDOWN) {
//                icon=R.drawable.arrowdoubledowngreen;
//            }else if(trend==Trend.SINGLEDOWN) {
//                icon=R.drawable.arrowdowngreen;
//            }else if(trend==Trend.FORTYFIVEDOWN) {
//                icon=R.drawable.arrow45downgreen;
//            }
//        }
//        return icon;
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping monitor " + monitorType + " for " + name);
        mNotifyMgr.cancel(deviceID);
        appContext.unregisterReceiver(alarmReceiver);
    }

    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.ktind.cgm.SNOOZE_ALARM")){
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
