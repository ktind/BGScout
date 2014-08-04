package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public interface CGMDeviceInterface {
    public DeviceDownloadObject download();
    void fireMonitors();
    void stopMonitors();
}
