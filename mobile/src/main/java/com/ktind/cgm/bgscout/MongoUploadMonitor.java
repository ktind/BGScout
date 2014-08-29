package com.ktind.cgm.bgscout;

import android.content.Context;
import android.util.Log;

import com.mongodb.*;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
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
public class MongoUploadMonitor extends AbstractMonitor {
    private static final String TAG = MongoUploadMonitor.class.getSimpleName();
//    Context appContext;


    MongoUploadMonitor(String name,int devID,Context c) {
        super(name,devID,c,"mongo_uploader");
        this.setAllowVirtual(false);
//        this.setMonitorType("mongo uploader");
    }

    @Override
    protected void doProcess(DownloadObject d) {
        String mongoURI = null;
        String collectionName=null;

        DBCollection deviceData;

//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
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
            mongoClient.close();
            try {
                savelastSuccessDate(d.getLastRecordReadingDate().getTime());
            } catch (NoDataException e) {
                Log.v(TAG,"No data in download to update last success time");
            }
        } catch (UnknownHostException e) {
            Log.e(TAG,"Unable to upload to mongoDB",e);
        } catch (MongoTimeoutException e){
            Log.w(TAG,"Mongo timeout");
        } catch (MongoException e){
            Log.w(TAG, "Mongo catch all exception: ",e);
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping monitor " + monitorType + " for " + name);
    }
}