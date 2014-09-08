package com.ktind.cgm.bgscout;

import android.content.Context;
import android.util.Log;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.WriteConcern;

import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
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
 */
public class MongoUploadMonitor extends AbstractMonitor {
    private static final String TAG = MongoUploadMonitor.class.getSimpleName();
//    Context context;


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

        mongoURI=sharedPref.getString(deviceIDStr+"_mongo_uri","");
        collectionName=sharedPref.getString(deviceIDStr+"_mongo_col","");
        MongoClientURI uri=null;
        if (mongoURI!=null && ! mongoURI.equals("")) {
            uri = new MongoClientURI(mongoURI);
            Log.w(TAG,"Mongo URI not set");
            NotifHelper.notify(context,"Mongo URI not set");
        } else {
            NotifHelper.clearMessage(context,"Mongo URI not set");
        }

        if (uri==null){
            NotifHelper.notify(context,"Bad mongo URI");
            return;
        } else {
            NotifHelper.clearMessage(context,"Bad mongo URI");
        }

        DB db;

        try{
            MongoClient mongoClient = new MongoClient(uri);
            db = mongoClient.getDB(uri.getDatabase());
            deviceData = db.getCollection(collectionName);
            ArrayList<EGVRecord> r=d.getEgvArrayListRecords();
            int uploadCount=0;
            for (EGVRecord sr:r) {
                BasicDBObject data = new BasicDBObject();
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
            Log.i(TAG,"Records processed: "+r.size()+" Records Uploaded: "+uploadCount);
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
            // Feels like a hack - need to figure out a better way to handle monitor error messages for the user
            NotifHelper.clearMessage(context,"Unable to upload to mongoDB");
            NotifHelper.clearMessage(context,"Mongo timeout");
            NotifHelper.clearMessage(context,"Mongo error");
            try {
                savelastSuccessDate(d.getLastRecordReadingDate().getTime());
            } catch (NoDataException e) {
                Log.v(TAG,"No data in download to update last success time");
            }
            //TODO Alert user when connection to mongo is unsuccessful
        } catch (UnknownHostException e) {
            Log.e(TAG,"Unable to upload to mongoDB",e);
            NotifHelper.notify(context,"Unable to upload to mongoDB");
        } catch (MongoTimeoutException e){
            Log.w(TAG,"Mongo timeout");
            NotifHelper.notify(context,"Mongo timeout");
        } catch (MongoException e){
            Log.w(TAG, "Mongo catch all exception: ",e);
            NotifHelper.notify(context,"Mongo error");
        }
    }

    @Override
    public void stop() {
        Log.i(TAG, "Stopping monitor " + monitorType + " for " + name);
    }

}