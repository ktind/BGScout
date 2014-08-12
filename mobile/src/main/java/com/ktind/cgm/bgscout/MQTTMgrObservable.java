package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/12/14.
 */
public interface MQTTMgrObservable {
    public void registerObserver(MQTTMgrObserverInterface observer);
    public void unregisterObserver(MQTTMgrObserverInterface observer);
    public void notifyObservers(String... messages);
}
