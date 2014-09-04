package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.mongodb.*;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
public class RemoteMongoDevice extends AbstractPollDevice {
    private static final String TAG = RemoteMongoDevice.class.getSimpleName();

    private String mongoURI = null;
    private String collectionName = null;

    private DBCollection deviceData;
    MongoClientURI uri=null;// = new MongoClientURI(mongoURI);
    DB db;
    MongoClient mongoClient = null;
    long lastQueryDate;
    EGVRecord[] lastRecord=new EGVRecord[1];

    public RemoteMongoDevice(String n,int deviceID,Context appContext){
        super(n,deviceID,appContext,"RemoteMongo");
        // Quasi race condition - CGM takes a second or 2 to read and upload while the "remote CGM" takes less time.
        // Give it some time to settle. If not it'll try again in 45 seconds.
        this.setPollInterval(304000);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
//        String[] device_list={"device_1","device_2","device_3","device_4"};
        for (String dev:Constants.DEVICES) {
            if (sharedPref.getString(dev+"_name","").equals(getName())){
                mongoURI=sharedPref.getString(dev+"_mongo_uri","");
                collectionName=sharedPref.getString(dev+"_mongo_col","");
            }
        }
        if (mongoURI!=null)
            uri = new MongoClientURI(mongoURI);
        // stop infinite loops!
        remote = true;
    }

    @Override
    public int getDeviceBattery() {
        return 100;
    }

    @Override
    public void connect() throws DeviceException {
    }

    @Override
    protected DownloadObject doDownload() {
        DownloadObject ddo=new DownloadObject();
        ddo.setDeviceID(getDeviceIDStr());
//        ddo.setDevice(this);
        ddo.setStatus(DownloadStatus.APPLICATIONERROR);
        ArrayList<EGVRecord> egvRecords=new ArrayList<EGVRecord>();
        ddo.setEgvRecords(new EGVRecord[0]);
        try {
            mongoClient = new MongoClient(uri);
            db = mongoClient.getDB(uri.getDatabase());
            deviceData = db.getCollection(collectionName);
            //FIXME Limit this unless we want to kill the heap over time...
            BasicDBObject query=new BasicDBObject("date", new BasicDBObject("$gt",lastQueryDate));
            DBCursor cursor=deviceData.find(query);
            try {
                List<DBObject> dbObjects=cursor.toArray();
                Log.d(TAG,"Size of response from mongo query: "+dbObjects.size());
                for (DBObject dbObject:dbObjects){
                    long recQueryDate=Long.valueOf(dbObject.get("date").toString());
                    Date recDate=new Date(recQueryDate);
                    int bgValue=Integer.valueOf(dbObject.get("sgv").toString());
                    Trend trend=Trend.values()[Integer.valueOf(dbObject.get("trend").toString())];
                    EGVRecord record;
                    if (recQueryDate>lastQueryDate){
                        lastQueryDate=recQueryDate;
                        record=new EGVRecord(bgValue,recDate,trend,true);
                        lastRecord[0]=record;
                    }else{
                        record=new EGVRecord(bgValue,recDate,trend,false);
                    }
                    egvRecords.add(record);
                    ddo.setEgvRecords(egvRecords.toArray(new EGVRecord[egvRecords.size()]));
                }
                ddo.setStatus(DownloadStatus.SUCCESS);
                //FIXME there has to be a more efficient way
                if (lastRecord!=null && ddo.getEgvRecords().length==0){
                    ddo.setStatus(DownloadStatus.NODATA);
                    ddo.setEgvRecords(lastRecord);
                }

            } finally {
                cursor.close();
            }
            Log.d(TAG, "Performing start of data from mongo for " + getName());
            mongoClient.close();
        }catch(UnknownHostException e){
            Log.e(TAG,"Unable to connect to MongoDB URI",e);
            ddo.setStatus(DownloadStatus.DEVICENOTFOUND);
            ddo.setEgvRecords(new EGVRecord[0]);
        }
        lastDownloadObject=ddo;
        return ddo;
    }

    @Override
    public void disconnect() {

    }
}