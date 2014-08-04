package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public interface MonitorInterface {
    public void process(DeviceDownloadObject d);
    public void stop();
    public void setHighThreshold(int highThreshold);
    public void setLowThreshold(int lowThreshold);
}
