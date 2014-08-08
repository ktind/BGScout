package com.ktind.cgm.bgscout;

import java.util.Date;

/**
 * Created by klee24 on 8/2/14.
 */
public class DeviceDownloadObject {
    private AbstractDevice device;
    private EGVRecord[] egvRecords;
    private DownloadStatus status;
    private String specialValueMessage;
    private Date lastDownloadDate;

    public Date getLastDownloadDate() {
        return lastDownloadDate;
    }

    public void setLastDownloadDate(Date lastDownloadDate) {
        this.lastDownloadDate = lastDownloadDate;
    }

    public String getSpecialValueMessage() {
        return specialValueMessage;
    }

    public void setSpecialValueMessage(String specialValueMessage) {
        this.specialValueMessage = specialValueMessage;
    }

    DeviceDownloadObject(AbstractDevice c,EGVRecord[] e, DownloadStatus s){
        super();
        setDevice(c);
        setEgvRecords(e);
        setStatus(s);
    }

    DeviceDownloadObject(){
        egvRecords=new EGVRecord[0];
    }

    public AbstractDevice getDevice() {
        return device;
    }

    public void setDevice(AbstractDevice device) {
        this.device = device;
    }

    public EGVRecord[] getEgvRecords() {
        return egvRecords;
    }

    public void setEgvRecords(EGVRecord[] egvRecords) {
        this.egvRecords = egvRecords;
    }

    public DownloadStatus getStatus() {
        return status;
    }

    public void setStatus(DownloadStatus status) {
        this.status = status;
    }

//    public boolean didFail(){
//        return this.getStatus()!=DownloadStatus.SUCCESS;
//    }
}
