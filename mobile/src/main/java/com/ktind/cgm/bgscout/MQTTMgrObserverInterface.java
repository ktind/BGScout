package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/12/14.
 */
public interface MQTTMgrObserverInterface {
    public void onMessage(String... messages);
}
