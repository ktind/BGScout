package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mongodb.*;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by klee24 on 7/27/14.
 */
public class MongoUploadMonitor extends AbstractMonitor {
    private static final String TAG = MongoUploadMonitor.class.getSimpleName();
//    Context appContext;


    MongoUploadMonitor(String name,int devID,Context c) {
        super(name,devID,c);
        this.setAllowVirtual(false);
        this.setMonitorType("mongo uploader");
    }

    @Override
    protected void doProcess(DownloadObject d) {
        String mongoURI = null;
        String collectionName=null;

        DBCollection deviceData;

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        String[] device_list={"device_1","device_2","device_3","device_4"};
        for (String dev:device_list) {
            if (sharedPref.getString(dev+"_name","").equals(getName())){
                mongoURI=sharedPref.getString(dev+"_mongo_uri","");
                collectionName=sharedPref.getString(dev+"_mongo_col","");
            }
        }
        MongoClientURI uri=null;
        if (mongoURI!=null)
            uri = new MongoClientURI(mongoURI);

        DB db;

        try{
            MongoClient mongoClient = new MongoClient(uri);
            db = mongoClient.getDB(uri.getDatabase());
            deviceData = db.getCollection(collectionName);
            EGVRecord[] r=d.getEgvRecords();
            int uploadCount=0;
            for (EGVRecord sr:r) {
                BasicDBObject data = new BasicDBObject();
                if (sr.isNew()) {
                    data.put("name", d.getDeviceName());
                    data.put("trend", sr.getTrend().getVal());

                    // NightScout comptability
                    data.put("device", "dexcom");
                    data.put("date", sr.getDate().getTime());
                    data.put("dateString", new SimpleDateFormat("MM/dd/yyy hh:mm:ss aa").format(sr.getDate()));
                    data.put("sgv", sr.getEgv());
                    data.put("direction", sr.getTrend().getNsString());

                    deviceData.update(data, data, true, false, WriteConcern.UNACKNOWLEDGED);
                    uploadCount+=1;
                    Log.v(TAG, "Added Record - EGV: " + sr.getEgv() + " Trend: " + sr.getTrend().getNsString() + " Date: " + new SimpleDateFormat("MM/dd/yyy hh:mm:ss aa").format(sr.getDate()));
                }
            }
            Log.i(TAG,"Records processed: "+r.length+" Records Uploaded: "+uploadCount);
            if (!d.isRemoteDevice()) {
                BasicDBObject data = new BasicDBObject();
                data.put("name", d.getDeviceName());
                data.put("deviceCheckinDate", new Date().getTime());
                data.put("uploaderBattery", d.getUploaderBattery());
                data.put("cgmbattery", d.getDeviceBattery());
                data.put("units", d.getUnit().getValue());
                data.put("downloadStatus", d.getStatus().toString());
                deviceData.update(data, data, true, false, WriteConcern.UNACKNOWLEDGED);
            }
            if (mongoClient != null)
                mongoClient.close();
        } catch (UnknownHostException e) {
            Log.e(TAG,"Unable to upload to mongoDB",e);
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping monitor " + monitorType + " for " + name);
    }
}