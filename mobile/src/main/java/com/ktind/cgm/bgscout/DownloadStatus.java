package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public enum DownloadStatus {
    SUCCESS("Successful download of data",0),
    NODATA("No records found",1),
    DEVICENOTFOUND("No device found",2),
    IOERROR("Read or write error",3),
    APPLICATIONERROR("Encountered an unknown error in application",4),
    SPECIALVALUE("Has a special value",5),
    NONE("No download has been performed",6),
    UNKNOWN("Unknown error",7);

    private String message;
    private int value;

    DownloadStatus(String message, int v){
        this.message=message;
        this.value=v;
    }

    public String toString() {
        return message;
    }

    public int getVal(){
        return value;
    }
}
