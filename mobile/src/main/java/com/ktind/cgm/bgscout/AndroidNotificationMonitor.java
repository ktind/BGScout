package com.ktind.cgm.bgscout;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * Created by klee24 on 8/2/14.
 */
public class AndroidNotificationMonitor extends AbstractMonitor {
    private static final String TAG = AndroidNotificationMonitor.class.getSimpleName();
    protected int notifID;
    protected Context appContext;
    protected Notification.Builder notifBuilder;
    protected NotificationManager mNotifyMgr;
    final protected String monitorType="android notification";

    public NotificationManager getmNotifyMgr() {
        return mNotifyMgr;
    }

    public void setmNotifyMgr(NotificationManager mNotifyMgr) {
        this.mNotifyMgr = mNotifyMgr;
    }

    public int getNotifID() {
        return notifID;
    }

    public void setNotifID(int notifID) {
        this.notifID = notifID;
    }

    public Context getAppContext() {
        return appContext;
    }

    public void setAppContext(Context appContext) {
        this.appContext = appContext;
    }

    public Notification.Builder getNotifBuilder() {
        return notifBuilder;
    }

    public void setNotifBuilder(Notification.Builder notifBuilder) {
        this.notifBuilder = notifBuilder;
    }

    AndroidNotificationMonitor(String name,int notifID,Context appContext){
        super(name);

        this.setNotifID(notifID);
//        this.name=name;
        this.setAppContext(appContext);
        this.setMonitorType("mongo uploader");
        mNotifyMgr = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent contentIntent = PendingIntent.getActivity(appContext, 0, new Intent(appContext, MainActivity.class), 0);
        Bitmap bm = BitmapFactory.decodeResource(appContext.getResources(), R.drawable.icon);
        this.setNotifBuilder(new Notification.Builder(appContext)
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(name)
                .setContentText("Monitor started. No data yet")
                .setContentIntent(contentIntent)
                .setOngoing(true)
//                .addAction(R.drawable.icon24x24, "Snooze", contentIntent)
                .setLargeIcon(bm));
        Notification notification = notifBuilder.build();
        mNotifyMgr.notify(notifID, notification);
        this.setAllowVirtual(true);
    }

    @Override
    public void doProcess(DeviceDownloadObject dl) {
        EGVRecord[] recs = dl.getEgvRecords();
        EGVRecord lastRec = null;
        if (recs != null && recs.length > 0)
            lastRec = recs[recs.length - 1];
        String msg="";

        Notification notification;
        int icon = R.drawable.questionmarkicon;
        Log.v(TAG,"Status: "+dl.getStatus().toString());
        if (dl.getStatus() != DownloadStatus.SUCCESS && dl.getStatus() != DownloadStatus.SPECIALVALUE) {
            msg = dl.getStatus().toString()+"\n";
            notifBuilder.setTicker(msg);
        }
        if (dl.getStatus() == DownloadStatus.SPECIALVALUE){
            msg = dl.getSpecialValueMessage()+"\n";
            notifBuilder.setTicker(msg);
        }

//        if (dl.getStatus()== DownloadStatus.NORECORDS){
//            msg+=dl.getStatus().toString();
//            notifBuilder.setTicker(msg);
//        }

        if (lastRec != null && dl.getStatus() != DownloadStatus.SPECIALVALUE) {
            icon = getIcon(lastRec.getEgv(), lastRec.getTrend());
            msg+= "BG: " + lastRec.getEgv() + " " + dl.getDevice().getUnit().toString() + " and " + lastRec.getTrend().toString();
//            msg+="\nLast reading: "+lastRec.getDate().toString();
            notifBuilder.setContentText(msg);
//                notifBuilder.setDefaults(Notification.DEFAULT_ALL);
        }
        notifBuilder.setStyle(new Notification.BigTextStyle().bigText(msg));
        notification = notifBuilder
                .setSmallIcon(icon)
                .build();
        mNotifyMgr.notify(notifID, notification);
    }


    private int getIcon(int bgValue,Trend trend){
        // Handle "NOT COMPUTABLE", "RATE OUT OF RANGE", and anything else that crops up.
        int icon=R.drawable.questionmarkicon;
        if (bgValue>=highThreshold){
            if (trend==Trend.NONE) {
                icon=R.drawable.nonered;
            }else if(trend==Trend.DOUBLEUP) {
                icon=R.drawable.arrowdoubleupred;
            }else if(trend==Trend.SINGLEUP) {
                icon=R.drawable.arrowupred;
            }else if(trend==Trend.FORTYFIVEUP) {
                icon=R.drawable.arrow45upred;
            }else if(trend==Trend.FLAT) {
                icon=R.drawable.arrowflatred;
            }else if(trend==Trend.DOUBLEDOWN) {
                icon=R.drawable.arrowdoubledownred;
            }else if(trend==Trend.SINGLEDOWN) {
                icon=R.drawable.arrowdownred;
            }else if(trend==Trend.FORTYFIVEDOWN) {
                icon=R.drawable.arrow45downred;
            }
        }else if (bgValue<=lowThreshold){
            if (trend==Trend.NONE) {
                icon=R.drawable.noneyellow;
            }else if(trend==Trend.DOUBLEUP) {
                icon=R.drawable.arrowdoubleupyellow;
            }else if(trend==Trend.SINGLEUP) {
                icon=R.drawable.arrowupyellow;
            }else if(trend==Trend.FORTYFIVEUP) {
                icon=R.drawable.arrow45upyellow;
            }else if(trend==Trend.FLAT) {
                icon=R.drawable.arrowflatyellow;
            }else if(trend==Trend.DOUBLEDOWN) {
                icon=R.drawable.arrowdoubledownyellow;
            }else if(trend==Trend.SINGLEDOWN) {
                icon=R.drawable.arrowdownyellow;
            }else if(trend==Trend.FORTYFIVEDOWN) {
                icon=R.drawable.arrow45downyellow;
            }
        }else{
            if (trend==Trend.NONE) {
                icon=R.drawable.noneblue;
            }else if(trend==Trend.DOUBLEUP) {
                icon=R.drawable.arrowdoubleupblue;
            }else if(trend==Trend.SINGLEUP) {
                icon=R.drawable.arrowupblue;
            }else if(trend==Trend.FORTYFIVEUP) {
                icon=R.drawable.arrow45upblue;
            }else if(trend==Trend.FLAT) {
                icon=R.drawable.arrowflatblue;
            }else if(trend==Trend.DOUBLEDOWN) {
                icon=R.drawable.arrowdoubledownblue;
            }else if(trend==Trend.SINGLEDOWN) {
                icon=R.drawable.arrowdownblue;
            }else if(trend==Trend.FORTYFIVEDOWN) {
                icon=R.drawable.arrow45downblue;
            }
        }
        return icon;
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping monitor " + monitorType + " for " + name);
        mNotifyMgr.cancel(notifID);
    }
}
