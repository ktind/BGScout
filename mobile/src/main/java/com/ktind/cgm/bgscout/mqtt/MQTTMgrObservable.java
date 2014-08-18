package com.ktind.cgm.bgscout.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by klee24 on 8/12/14.
 */
public interface MQTTMgrObservable {
    public void registerObserver(MQTTMgrObserverInterface observer);
    public void unregisterObserver(MQTTMgrObserverInterface observer);
    public void notifyObservers(String topic, MqttMessage message);
}
