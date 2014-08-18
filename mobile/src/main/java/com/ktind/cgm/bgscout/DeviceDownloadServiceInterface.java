package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public interface DeviceDownloadServiceInterface {
    public DownloadObject downloadDevice();
    public void processDownload(DownloadObject d);
}
