package com.ktind.cgm.bgscout;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.ktind.cgm.bgscout.mqtt.MQTTMgr;
import com.ktind.cgm.bgscout.mqtt.MQTTMgrObserverInterface;

import org.eclipse.paho.client.mqttv3.MqttMessage;

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
public class MqttUploader extends AbstractMonitor implements MQTTMgrObserverInterface {
    private static final String TAG = MqttUploader.class.getSimpleName();
    protected DownloadObject lastDownload=null;
    protected boolean initialUpload=true;

    protected MQTTMgr mqttMgr;

    public MqttUploader(String n, int devID ,Context context) {
        super(n,devID,context,"mqtt_uploader");
//        this.setMonitorType("MQTT uploader");
//        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
        String usr=sharedPref.getString(deviceIDStr+"_mqtt_user","");
        String pw=sharedPref.getString(deviceIDStr+"_mqtt_pass","");
        mqttMgr=new MQTTMgr(appContext,usr,pw,getDeviceIDStr());
        mqttMgr.initConnect(url);
        mqttMgr.registerObserver(this);
        this.allowVirtual=false;
    }

    @Override
    protected void doProcess(DownloadObject d) {
        Gson gson=new Gson();
        if (initialUpload || (lastDownload!=null && ! lastDownload.equals(d))) {
            mqttMgr.publish(gson.toJson(d), "/entries/sgv");
            initialUpload=false;
            try {
                savelastSuccessDate(d.getLastRecordReadingDate().getTime());
            } catch (NoDataException e) {
                Log.v(TAG,"No data in download to update last success time");
            }
        }else {
            Log.i(TAG,"Nothing to publish because there was no change in state" );
        }
        lastDownload=d;
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
