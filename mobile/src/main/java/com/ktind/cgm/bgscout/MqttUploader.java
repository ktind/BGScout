package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
//import android.util.Log;

import com.ktind.cgm.bgscout.mqtt.MQTTMgr;
import com.ktind.cgm.bgscout.mqtt.MQTTMgrObserverInterface;

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONObject;

/**
 * Created by klee24 on 8/7/14.
 */
public class MqttUploader extends AbstractMonitor implements MQTTMgrObserverInterface {
    private static final String TAG = MqttUploader.class.getSimpleName();

    protected MQTTMgr mqttMgr;

    public MqttUploader(String n, int devID ,Context context) {
        super(n,devID,context);
        this.setMonitorType("MQTT uploader");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
        String usr=sharedPref.getString(deviceIDStr+"_mqtt_user","");
        String pw=sharedPref.getString(deviceIDStr+"_mqtt_pass","");
        mqttMgr=new MQTTMgr(appContext,usr,pw,getDeviceIDStr());
        mqttMgr.connect(url,"crooak...");
        mqttMgr.registerObserver(this);
        this.allowVirtual=false;
//        mqttMgr.subscribe("entries/sgv");
    }

    @Override
    protected void doProcess(DownloadObject d) {
        d.buildMessage();
        JSONObject json=d.getJson();
        mqttMgr.publish(json.toString(),"/entries/sgv");
    }

    @Override
    public void stop() {
        super.stop();
        mqttMgr.disconnect();
        mqttMgr.unregisterObserver(this);
    }

    @Override
    public void onMessage(String topic, MqttMessage message) {

    }
}
