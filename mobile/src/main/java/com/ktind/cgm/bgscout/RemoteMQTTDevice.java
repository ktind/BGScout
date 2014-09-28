package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.protobuf.InvalidProtocolBufferException;
import com.ktind.cgm.bgscout.mqtt.MQTTMgr;
import com.ktind.cgm.bgscout.mqtt.MQTTMgrObserverInterface;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
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
public class RemoteMQTTDevice extends AbstractPushDevice implements MQTTMgrObserverInterface {
    private static final String TAG = RemoteMQTTDevice.class.getSimpleName();

    private MQTTMgr mqttMgr;

    public RemoteMQTTDevice(String n, int deviceID, Context appContext) {
        super(n, deviceID, appContext, "RemoteMQTT");
        setDeviceType("Remote MQTT");
        setRemote(true);
    }

//    @Override
//    public void onDataReady(DownloadObject ddo) {
//    }

    @Override
    public int getDeviceBattery() throws IOException {
        return 0;
    }

    @Override
    public void start() {
        super.start();
        connect();
        state=State.STARTED;
    }

    @Override
    public void connect() {
        Log.d(TAG,"Connect started");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        final String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
        String usr=sharedPref.getString(deviceIDStr+"_mqtt_user","");
        String pw=sharedPref.getString(deviceIDStr+"_mqtt_pass","");
        mqttMgr=new MQTTMgr(context,usr,pw,getDeviceIDStr());

        new Thread(new Runnable() {
            @Override
            public void run() {
                mqttMgr.initConnect(url);
                Log.d(TAG, "Subscribe start");
//        mqttMgr.subscribe("/entries/sgv", "/uploader");
                mqttMgr.subscribe("/entries/sgv",MqttUploader.PROTOBUF_DOWNLOAD_TOPIC);
                Log.d(TAG,"Connect ended");
            }
        }).start();

        mqttMgr.registerObserver(this);
    }

    @Override
    public void disconnect() {
        if (mqttMgr==null)
            return;
        mqttMgr.disconnect();
        mqttMgr.close();
        mqttMgr.unregisterObserver(this);
        mqttMgr=null;
    }

    @Override
    public void stop() {
        super.stop();
        disconnect();
        state=State.STOPPED;
    }

    @Override
    public void onMessage(String topic, MqttMessage msg) {
        Log.d(TAG,"Received message for topic "+topic);
//        if (topic.equals("/uploader")){
//            Log.w(TAG,"Uploader lost connectivity");
//        }
        if (topic.equals("/entries/sgv")) {
            GsonBuilder gsonb=new GsonBuilder();
            gsonb.registerTypeAdapter(GlucoseUnit.class, new GlucoseUnit.GlucoseUnitSerializer());
            gsonb.registerTypeAdapter(GlucoseUnit.class, new GlucoseUnit.GlucoseUnitDeSerializer());
            Gson gson=gsonb.create();
            Log.d(TAG,"message: "+new String(msg.getPayload()));
            DownloadObject ddo=gson.fromJson(new String(msg.getPayload()),DownloadObject.class);
            Log.d(TAG,"Message from device: "+ddo.getDeviceName());
            setLastDownloadObject(ddo);
            onDownload(ddo);
        }
        if (topic.equals("/downloads/protobuf")){
            try {
                SGV.CookieMonsterG4Download protoBufDownload=SGV.CookieMonsterG4Download.parseFrom(msg.getPayload());
                Log.d("XXX", "SGV Download status: "+protoBufDownload.getDownloadStatus().name());
                Log.d("XXX", "SGV Units: " + protoBufDownload.getUnits().name());
                Log.d("XXX", "SGV Download timestamp: "+new Date(protoBufDownload.getDownloadTimestamp()));
                Log.d("XXX", "SGV Uploader battery: " + protoBufDownload.getUploaderBattery());
                Log.d("XXX", "SGV BG: "+protoBufDownload.getSgv(0).getSgv());
                Log.d("XXX", "SGV direction: "+protoBufDownload.getSgv(0).getDirection().name());
                Log.d("XXX", "SGV timestamp: "+new Date(protoBufDownload.getSgv(0).getTimestamp()));

                ArrayList<EGVRecord> egvRecords = new ArrayList<EGVRecord>();
                for (SGV.CookieMonsterSGVG4 sgv: protoBufDownload.getSgvList()){
                    egvRecords.add(new EGVRecord(sgv.getSgv(),sgv.getTimestamp(),Trend.values()[sgv.getDirection().getNumber()]));
                }
                DownloadObject downloadObject=new DownloadObject();
                // TODO: once more CGMS are decoded we should consider adding some of these to the Protobuf definition.
                downloadObject.setUploaderBattery(protoBufDownload.getUploaderBattery())
                        .setDownloadDate(new Date(protoBufDownload.getDownloadTimestamp()))
                        .setEgvRecords(egvRecords)
                        .setRemoteDevice(false)
                        .setDeviceName("dexcom")
                        .setDeviceBattery(protoBufDownload.getReceiverBattery())
                        .setUploaderBattery(protoBufDownload.getUploaderBattery())
                        .setUnit(GlucoseUnit.values()[protoBufDownload.getUnits().getNumber()])
                        .setDeviceID("device_1")
                        .setLastReadingDate(egvRecords.get(egvRecords.size()-1).getDate())
                        .setDriver("DexcomG4");
                setLastDownloadObject(downloadObject);
                onDownload(downloadObject);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDisconnect() {
        lastDownloadObject.setStatus(DownloadStatus.REMOTEDISCONNECTED);
//        fireMonitors(lastDownloadObject);
    }
}
