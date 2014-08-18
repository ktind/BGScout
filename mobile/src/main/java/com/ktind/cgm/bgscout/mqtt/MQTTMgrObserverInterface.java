package com.ktind.cgm.bgscout.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by klee24 on 8/12/14.
 */
public interface MQTTMgrObserverInterface {
    public void onMessage(String topic, MqttMessage message);
}
