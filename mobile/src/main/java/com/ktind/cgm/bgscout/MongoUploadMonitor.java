package com.ktind.cgm.bgscout;

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

    MongoUploadMonitor(String name) {
        super(name);
        this.setAllowVirtual(false);
        this.setMonitorType("mongo uploader");
    }

    @Override
    protected void doProcess(DeviceDownloadObject d) {
        //FIXME add these as user configurable options
        String mongoURI = "";
        String collectionName="";
        DBCollection deviceData;

        MongoClientURI uri = new MongoClientURI(mongoURI);
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
                    //Fixme: need to be a separate document
                    data.put("name", d.getDevice().getName());
//                    data.put("cgmbattery", d.getDevice().getCGMBattery());
//                    data.put("uploaderBattery", d.getDevice().getUploaderBattery());
//                    data.put("units", d.getDevice().getUnit().getValue());

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
            if (!d.getDevice().isVirtual()) {
                BasicDBObject data = new BasicDBObject();
                data.put("name", d.getDevice().getName());
                data.put("deviceCheckinDate", new Date().getTime());
                data.put("uploaderBattery", d.getDevice().getUploaderBattery());
                data.put("cgmbattery", d.getDevice().getCGMBattery());
                data.put("units", d.getDevice().getUnit().getValue());
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