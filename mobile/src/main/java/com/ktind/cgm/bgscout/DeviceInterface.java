package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public interface DeviceInterface {
    // FIXME this both returns a value and sets an internal variable - bad form.
    public void start();
    public void stop();
    void fireMonitors();
    void stopMonitors();
}
