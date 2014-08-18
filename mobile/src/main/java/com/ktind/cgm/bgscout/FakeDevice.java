package com.ktind.cgm.bgscout;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

/**
 * Created by klee24 on 8/2/14.
 */
public class FakeDevice extends AbstractPollDevice {
    private static final String TAG = DeviceDownloadService.class.getSimpleName();
    private ArrayList<EGVRecord> egvHistory=new ArrayList<EGVRecord>(FakeCGMDeviceConstants.MAXEGV);
    private boolean initialRun;
    private Date lastReading=new Date(new Date().getTime()-10800000L);

    public FakeDevice(String n, int deviceID, Context appContext, Handler mH) {
        //    public AbstractCGMDevice(String n,int deviceID,Context appContext){
        super(n,deviceID,appContext,mH);
//        generateEGVHistory();
        initialRun=true;
        remote =false;
        this.pollInterval=15000;
        this.deviceType="Fake device";
    }

    @Override
    public int getDeviceBattery() {
        return 0;
    }

    @Override
    protected DownloadObject doDownload() {
        return generateDownloadObject();
    }

    @Override
    public void connect() throws DeviceException, IOException {
        super.connect();
    }

    @Override
    public void disconnect() {
    }


    private DownloadObject generateDownloadObject(){
        Log.d(TAG,"Generating start object");
//        if (!initialRun)
//            this.addEGV();
        DownloadStatus downloadStatus=generateStatus();
        EGVRecord[] egvArray=new EGVRecord[1];
        Random rand=new Random();
        egvArray[0]=new EGVRecord(rand.nextInt(362)+39,new Date(),Trend.FLAT,true);
        egvHistory.add(egvArray[0]);

        DownloadObject ddo=new DownloadObject(this,egvArray,downloadStatus);
        lastDownloadObject=ddo;
        initialRun=false;
        return ddo;
    }

    @Override
    public void fireMonitors() {
        super.fireMonitors();
        lastReading=egvHistory.get(egvHistory.size()-1).getDate();
//        for (EGVRecord r:egvHistory){
//            r.setNew(false);
//        }
    }

    private void addEGV(){
        egvHistory.remove(0);
        for (EGVRecord r:egvHistory){
            r.setNew(false);
        }
        int lastIndex=egvHistory.size()-1;
        int lastBG=egvHistory.get(lastIndex).getEgv();
        Trend lastTrend=egvHistory.get(lastIndex).getTrend();
        Date lastDate=egvHistory.get(lastIndex).getDate();
        Log.d(TAG,"Last reading. BG: "+lastBG+" Trend: "+lastTrend.toString()+" Date: "+lastDate);
        EGVRecord record=generateNextEGV(lastBG, lastTrend, lastDate);
        if (record.getDate().after(lastReading))
            record.setNew(true);
        egvHistory.add(record);
    }

    // This patient doesn't seem to mind extreme highs or lows so you won't
    // see the normal patterns for corrections
    private void generateEGVHistory(){
        Log.d(TAG,"Generating new EGV History");
        Random rand=new Random();
        int initialBG=rand.nextInt(FakeCGMDeviceConstants.MAXEGV+1)+ FakeCGMDeviceConstants.MINEGV;
        Trend initialTrend=generateInitialTrend();
        long initialMillis=1000*(FakeCGMDeviceConstants.READINGINTERVALSECONDS* FakeCGMDeviceConstants.MAXRECORDS);
        Date initialDate=new Date(new Date().getTime()-initialMillis);
        egvHistory.add(generateEGV(initialBG, initialTrend, initialDate, true));
//        int missedCounter=0;
        for (int i=1;i< FakeCGMDeviceConstants.MAXRECORDS;i++){
            Log.d(TAG,"Counter: "+i+" egvHistory.size(): "+egvHistory.size());
            if (rand.nextFloat()> FakeCGMDeviceConstants.MISSEDREADINGRATE) {
                int lastIndex=egvHistory.size()-1;
                int lastBG = egvHistory.get(lastIndex).getEgv();
                Trend lastTrend = egvHistory.get(lastIndex).getTrend();
                Date date = new Date(egvHistory.get(lastIndex).getDate().getTime() - (1000 * FakeCGMDeviceConstants.READINGINTERVALSECONDS * (FakeCGMDeviceConstants.MAXRECORDS - i)));
                egvHistory.add(generateNextEGV(lastBG, lastTrend, date));
//                missedCounter+=1;
            }
        }
    }

    private DownloadStatus generateStatus(){
        DownloadStatus status=generateStatus(FakeCGMDeviceConstants.FAILRATE);
        return status;
    }

    private DownloadStatus generateStatus(float failRate){
        Random rand=new Random();
        DownloadStatus status=DownloadStatus.SUCCESS;
        float check=rand.nextFloat();
        Log.d(TAG,"Checking for failure: "+check+" Rate is: "+failRate);
        if (check<failRate) {
            Log.d(TAG,"Fail triggered");
            status = DownloadStatus.values()[rand.nextInt(DownloadStatus.values().length)];
        }
        Log.d(TAG,"Randomly generated start status: "+status.toString());
        return status;
    }

    private Trend generateInitialTrend(){
        Random rand=new Random();
        Trend t=Trend.values()[rand.nextInt(Trend.values().length)];
        return t;
    }

    // Generate the next trend based on the last trend
    private Trend generateNextTrend(Trend lastTrend){
        Random rand=new Random();
        Trend trend=lastTrend;

        // Set a 40% chance of change in direction
        if (rand.nextFloat()<0.40f){
            int change;
            // Only change direction to a valid value.
            if (lastTrend.getVal()==0) {
                change = 1;
            } else if (lastTrend.getVal()==Trend.values().length-1) {
                change = -1;
            } else {
                change = rand.nextInt(3)-1;
            }
            int newTrendIndex=lastTrend.getVal()+change;
            trend=Trend.values()[newTrendIndex];
        }
        Log.d(TAG,"Randomly generated trend: "+trend.toString());
        return trend;

    }
    private EGVRecord generateNextEGV(int lastBG,Trend lastTrend,Date date){
        return generateEGV(lastBG,lastTrend,date,true);
    }


    private EGVRecord generateEGV(int lastBG,Trend lastTrend,Date date,boolean isNew){
        Random rand=new Random();
        EGVRecord record=new EGVRecord();
        Trend trend=this.generateNextTrend(lastTrend);
        record.setTrend(trend);
        record.setDate(date);
        record.setNew(isNew);
        int changeRate=1;
        boolean negTrend=false;
        switch (lastTrend){
            case FLAT:
                changeRate=rand.nextInt(2);
                if (rand.nextFloat()<0.50f) {
                    negTrend=true;
                }
                break;
            case FORTYFIVEUP:
                changeRate=rand.nextInt(2)+1;
                break;
            case SINGLEUP:
                changeRate=rand.nextInt(2)+2;
                break;
            case NONE:
                changeRate=1;
                break;
            case DOUBLEUP:
                changeRate=rand.nextInt(5)+3;
                break;
            case FORTYFIVEDOWN:
                changeRate=rand.nextInt(2)+1;
                negTrend=true;
                break;
            case SINGLEDOWN:
                changeRate=rand.nextInt(2)+2;
                negTrend=true;
                break;
            case DOUBLEDOWN:
                changeRate=rand.nextInt(5)+3;
                negTrend=true;
                break;
            case NOTCOMPUTE:
                // not going to do anything here
                changeRate=1;
                break;
            case RATEOUTRANGE:
                changeRate=rand.nextInt(9)+7;
                // 50/50 chance that it will be either negative or positive. I don't want to have
                // to keep the state of the previous trend when we're already going off the last
                // trend. This is a simple test device for now.
                if (rand.nextFloat()>0.50f){
                    negTrend=true;
                }
                break;
        }
        if (changeRate==0)
            changeRate=1;
        int bgChange=rand.nextInt(changeRate*(FakeCGMDeviceConstants.READINGINTERVALSECONDS/60));
        if (negTrend)
            bgChange=bgChange*-1;
        int newEGV=lastBG+bgChange;
        if (newEGV< FakeCGMDeviceConstants.MINEGV)
            newEGV= FakeCGMDeviceConstants.MINEGV;
        if (newEGV> FakeCGMDeviceConstants.MAXEGV)
            newEGV= FakeCGMDeviceConstants.MAXEGV;
        record.setEgv(newEGV);
        Log.d(TAG,"Generated EGV. BG: "+record.getEgv()+" Trend: "+record.getTrend().toString()+" Date: "+record.getDate().toString());
        return record;
    }
}
