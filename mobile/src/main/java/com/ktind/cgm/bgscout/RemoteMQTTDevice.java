package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.ktind.cgm.bgscout.mqtt.MQTTMgr;
import com.ktind.cgm.bgscout.mqtt.MQTTMgrObserverInterface;

import java.io.IOException;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by klee24 on 8/6/14.
 */
public class RemoteMQTTDevice extends AbstractPushDevice implements MQTTMgrObserverInterface {
    private static final String TAG = RemoteMQTTDevice.class.getSimpleName();

    private MQTTMgr mqttMgr;

    public RemoteMQTTDevice(String n, int deviceID, Context appContext, Handler mH) {
        super(n, deviceID, appContext, mH);
        setDeviceType("Remote MQTT");
        setRemote(true);
    }

    @Override
    public void onDataReady(DownloadObject ddo) {
    }

    @Override
    public int getDeviceBattery() throws IOException {
        return 0;
    }

    @Override
    public void start() {
        super.start();
        try {
            connect();
        } catch (DeviceException deviceException) {
            deviceException.printStackTrace();
        }
    }

    @Override
    public void connect() throws DeviceException {
        Log.d(TAG,"Connect started");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
        String usr=sharedPref.getString(deviceIDStr+"_mqtt_user","");
        String pw=sharedPref.getString(deviceIDStr+"_mqtt_pass","");
        mqttMgr=new MQTTMgr(appContext,usr,pw,getDeviceIDStr());
        mqttMgr.initConnect(url);
        mqttMgr.registerObserver(this);
        Log.d(TAG, "Subscribe start");
//        mqttMgr.subscribe("/entries/sgv", "/uploader");
        mqttMgr.subscribe("/entries/sgv");
        Log.d(TAG,"Connect ended");
    }

    @Override
    public void disconnect() {
        mqttMgr.disconnect();
        mqttMgr.unregisterObserver(this);
        mqttMgr=null;
    }

    @Override
    public void stop() {
        super.stop();
        disconnect();
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
            onDownload();
        }
    }
}
