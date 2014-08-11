package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public enum DownloadStatus {
    SUCCESS("Successful start of data"),
    NORECORDS("No records found"),
    DEVICENOTFOUND("No device found"),
    IOERROR("Read or write error"),
//    READERROR("Error reading from device"),
//    WRITEERROR("Error writing to device"),
    APPLICATIONERROR("Encountered an unknown error in application"),
    NONE("No download has been performed"),
    SPECIALVALUE("Has a special value");

    private String message;

    DownloadStatus(String message){
        this.message=message;
    }

    public String toString() {
        return message;
    }
}
