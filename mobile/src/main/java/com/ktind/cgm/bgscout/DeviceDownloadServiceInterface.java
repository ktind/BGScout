package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public interface DeviceDownloadServiceInterface {
    public DeviceDownloadObject downloadDevice();
    public void processDownload(DeviceDownloadObject d);
}
