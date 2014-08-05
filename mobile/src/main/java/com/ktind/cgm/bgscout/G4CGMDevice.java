package com.ktind.cgm.bgscout;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;


/**
 * Created by klee24 on 8/2/14.
 */
public class G4CGMDevice extends AbstractCGMDevice  {
    protected String serialNum;
    protected String receiverID;
    protected int cgmBattery=-1;
    private static final String TAG = G4CGMDevice.class.getSimpleName();
    // TODO add this as a shared preference...
    protected Date myLastDownload=new Date(new Date().getTime()-10800000);

    //    public AbstractCGMDevice(String n,int deviceID,Context appContext){

    public String getSerialNum() {
        return serialNum;
    }

    public void setSerialNum(String serialNum) {
        this.serialNum = serialNum;
    }

    public G4CGMDevice(String name,int devID,Context appContext,Handler mH){
        super(name,devID,appContext,mH);
//        this.appContext=context;
        virtual = false;
        cgmTransport=new G4USBSerialTransport(appContext);
    }

    @Override
    int getCGMBattery() throws IOException {
        if (cgmBattery==-1)
            cgmBattery=getBatteryLevel();
        return cgmBattery;
    }

    @Override
    public void connect() throws DeviceNotConnected {
        cgmTransport.open();
    }

    public void setup() throws IOException {
        if (isConnected()) {
            unit = getGlucoseUnit();
            serialNum = getRcvrSerial();
            cgmBattery=getBatteryLevel();
//            myBattery=getUploaderBattery();

        }else{
            Log.e("G4Device","Unable to setup device that I am not connected to");
        }
    }

    @Override
    protected DeviceDownloadObject doDownload() {
        DeviceDownloadObject ddo=new DeviceDownloadObject();
        ddo.setDevice(this);
        if (lastDownloadObject==null)
            lastDownloadObject=ddo;
        try {
            cgmTransport.open();
            this.setup();
            EGVRecord[] egvArray = this.getLastEGVRecords();
            getReadingsSince(myLastDownload, egvArray);
            int batteryLevel = this.getBatteryLevel();
            float myBattery= getUploaderBattery();
            Log.d(TAG, "Device battery level: " + batteryLevel);
            Log.d(TAG, "Phone battery level: " + myBattery);
            if (batteryLevel < 40) {
                if (myBattery > 0.40) {
                    Log.d(TAG, "Setting phone to charge device");
                    this.setChargeDevice(true);
                } else {
                    Log.d(TAG, "G4 battery low but this device is too low to charge it");
                    this.setChargeDevice(false);
                }
            } else {
                Log.d(TAG, "Stopping this device from charging G4");
                this.setChargeDevice(false);
            }
            try {
                Date dispDate=getDisplayTime();
                Long jitter=dispDate.getTime()-new Date().getTime();
                if (Math.abs(jitter) > 30000 ) {
                    Log.w(TAG,"Device time off by "+jitter+" ms");
                    this.syncTimeToDevice();
                }
            }catch (IOException e){
                Log.e(TAG,"Unable to syncTime to device");
            }
            cgmTransport.close();
            // FIXME reflect actual status
            DownloadStatus downloadStatus = DownloadStatus.SUCCESS;
            ddo = new DeviceDownloadObject(this, egvArray, downloadStatus);
            lastDownloadObject = ddo;
            myLastDownload=lastDownloadObject.getEgvRecords()[lastDownloadObject.getEgvRecords().length-1].getDate();
        } catch (DeviceNotConnected e){
            Log.d(TAG, "Not finding device here!");
            EGVRecord[] records=lastDownloadObject.getEgvRecords();
//            records=lastDownloadObject.getEgvRecords();
            ddo.setEgvRecords(records);
            lastDownloadObject=ddo;
            ddo.setStatus(DownloadStatus.DEVICENOTFOUND);
        } catch (IOException e){
            EGVRecord[] records=lastDownloadObject.getEgvRecords();
            ddo.setEgvRecords(records);
            lastDownloadObject=ddo;
            ddo.setStatus(DownloadStatus.IOERROR);
        }

        for (G4EGVSpecialValue specialValue:G4EGVSpecialValue.values()) {
            EGVRecord[] records=lastDownloadObject.getEgvRecords();
            if (records!=null && records.length>0) {
                EGVRecord rec = records[records.length - 1];
                if (rec.getEgv() == specialValue.getValue()) {
                    ddo.setStatus(DownloadStatus.SPECIALVALUE);
                    ddo.setSpecialValueMessage(G4EGVSpecialValue.getEGVSpecialValue(rec.getEgv()).toString());
                    break;
                }
            }
        }

        return ddo;
//        return super.doDownload();
    }

    @Override
    public void disconnect() {
        cgmTransport.close();
    }

    public void setChargeDevice(boolean c){
        cgmTransport.setChargeReceiver(c);
    }

//    @Override
    public G4EGVRecord[] getReadings() {
        return this.getReadings(G4Constants.defaultReadings);
    }

    public G4EGVRecord[] getReadings(int numReadings) {
        return new G4EGVRecord[0];
    }

    // Retrieves the last 4 pages
    public G4EGVRecord[] getLastEGVRecords() throws IOException {
        G4Partition partition=getDBPageRange(G4RecType.EGVDATA);
        G4EGVRecord[] results=getEGVPages(partition.lastPage - 3, 4);
        return results;
    }

    // TODO Possibly abstract this out and have it return a collection of parsed records?
    public G4EGVRecord[] getEGVPages(int firstPage,int lastPage) throws IOException {

        G4DBPage[] pages=getDBPages(G4RecType.EGVDATA,firstPage,lastPage);
        int totalNumRecords=0;
        for (G4DBPage page: pages){
            totalNumRecords+=page.PageHeader.NumberOfRecords;
        }

        Log.d(TAG,"Record type: "+pages[0].PageHeader.RecordType.toString()+"PageCount: "+pages.length+"Total records: "+totalNumRecords);
        G4EGVRecord[] egvrecords = new G4EGVRecord[totalNumRecords];
        int i=0;
        for (G4DBPage page: pages) {
            G4EGVRecord[] tmprecs = new G4EGVRecord[page.PageHeader.NumberOfRecords];
            tmprecs=parsePage(page);
            System.arraycopy(tmprecs,0,egvrecords,i,page.PageHeader.NumberOfRecords);
            Log.d(TAG,"Start index: "+i+" Record count: "+page.PageHeader.NumberOfRecords+" End: "+(page.PageHeader.NumberOfRecords*i+page.PageHeader.NumberOfRecords));
            i+=page.PageHeader.NumberOfRecords;
        }
        return egvrecords;
    }

    public G4EGVRecord lastReading() throws IOException {
        G4EGVRecord[] results=getLastEGVRecords();
        if (results==null | results.length<1)
            return null;
        return results[results.length-1];
    }

    public EGVRecord[] getReadingsSince(Date d) throws IOException {
        // TODO continue to go back in time until we find the earliest record.
        G4EGVRecord[] recs=getLastEGVRecords();
        return getReadingsSince(d,recs);
    }

    //FIXME: clean this up. There shouldn't be a separate method to set this value should there? Violates SRP
    public EGVRecord[] getReadingsSince(Date d,EGVRecord[] recs){
        ArrayList<EGVRecord> resultsArrayList=new ArrayList<EGVRecord>();
        Log.d(TAG,"getReadingsSince date=> "+d);
        for (EGVRecord record:recs){
            if (record.getDate().after(d)) {
                record.setNew(true);
            } else {
                record.setNew(false);
            }
            resultsArrayList.add(record);
        }
        return resultsArrayList.toArray(new EGVRecord[resultsArrayList.size()]);
    }

    public boolean ping() throws IOException {
        if (!isConnected()){
            Log.e(TAG,"Ping failed - not connected to device");
            return false;
        }
        writeCmd(G4RcvrCmd.PING);
        byte[] result=readResponse();
        if (result==null || result.length!=6){
            Log.e(TAG,"Ping unsuccessful");
            return false;
        }
        Log.i(TAG,"Ping successful");
        return true;
    }

    public G4Partition getDBPageRange(G4RecType recordType) throws IOException {
        G4Partition response=new G4Partition();
        writeCmd(G4RcvrCmd.READDATABASEPAGERANGE,recordType.getValue());
        byte[] responseBuff = readResponse();
        if (responseBuff==null || responseBuff.length!=8){
            throw new IOException("Problem reading response");
//            return null;
        }
        byte[] firstPage = {responseBuff[0],responseBuff[1],responseBuff[2],responseBuff[3]};
        byte[] lastPage = {responseBuff[4],responseBuff[5],responseBuff[6],responseBuff[7]};
        response.Partition=recordType;
        response.firstPage= BitTools.byteArraytoInt(firstPage);
        response.lastPage= BitTools.byteArraytoInt(lastPage);
        Log.d(TAG,"Partition: "+response.Partition.toString()+"First Page: "+response.firstPage+"Last Page: "+response.lastPage);
        return response;
    }

    // There has to be a better way to do this?
    private void writeCmd(G4RcvrCmd cmd, byte value) throws IOException {
        byte[] b=new byte[1];
        b[0]=value;
        writeCmd(cmd,b);
    }

    public GlucoseUnit getGlucoseUnit() throws IOException {
        writeCmd(G4RcvrCmd.READGLUCOSEUNIT);
        byte[] res=readResponse();
        int result=0;
        if (res.length>0)
            result=(int) res[0];
        return GlucoseUnit.values()[result];
    }

    public String getTransmitterId() throws IOException {
        writeCmd(G4RcvrCmd.READTRANSMITTERID);
        String result=null;
        try {
            result = new String(readResponse(), "UTF-8");
        }catch(UnsupportedEncodingException e){
            Log.e(TAG,"Exception converting byte array to String",e);
        }
        return result;
    }

    public String getBatteryState() throws IOException {
        writeCmd(G4RcvrCmd.READBATTERYSTATE);
        String result="Unable to determine battery state";
        byte[] response=readResponse();

        if (response!=null && response.length>=0)
            result= G4BatteryState.values()[response[0]-1].toString();
        return result;
    }

    public int getBatteryLevel() throws IOException {
        writeCmd(G4RcvrCmd.READBATTERYLEVEL);
        int result=0;
        result = BitTools.byteArraytoInt(readResponse());
        return result;
    }

    public Date getDisplayTime() throws IOException {
        return new Date(getDisplayTimeLong());
    }

    public void syncTimeToDevice() throws IOException {
        Calendar mCalendar = new GregorianCalendar();
        TimeZone mTimeZone = mCalendar.getTimeZone();
        // TODO add protections against the device settings its time pre Jan 1 2009...
        long dispTimeOffset=new Date().getTime()/1000-G4Constants.RECEIVERBASEDATE/1000-getSystemTimeLong();
        // TODO Test daylight time change.
        // TODO Also test daylight time change and time zones that don't observe daylight time
        if (mTimeZone.inDaylightTime(new Date())){
            dispTimeOffset+=3600L; // 1 hour for daylight time if it is observed
        }
        // TODO switch everything to ByteBuffers - all the things.
        byte[] byteArray=ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt((int) dispTimeOffset).array();
//        Log.d(TAG,"Hex sync time(Little): "+ BitTools.bytesToHex(byteArray));
        writeCmd(G4RcvrCmd.WRITEDISPLAYTIMEOFFSET, byteArray);
        // TODO add verification that device did receive the command?
        byte[] resp=readResponse();
        Log.i(TAG,"Sync'd device time with cell. Set display time offset to: "+dispTimeOffset);
    }

    public long getDisplayTimeLong() throws IOException {
        Calendar mCalendar = new GregorianCalendar();
        TimeZone mTimeZone = mCalendar.getTimeZone();
        long dispTime=G4Constants.RECEIVERBASEDATE+getDisplayTimeOffsetLong()*1000L+getSystemTimeLong()*1000L;
        if (mTimeZone.inDaylightTime(new Date()))
            dispTime-=3600000L;
        Log.d(TAG,"getDisplayTimeLong: "+dispTime);
        return dispTime;
    }

    public long getSystemTimeLong() throws IOException {
        writeCmd(G4RcvrCmd.READSYSTEMTIME);
        long result=0;
        result = BitTools.byteArraytoInt(readResponse());
        Log.d(TAG,"getSystemTimeLong=>"+result);
        return result;
    }

    protected long getDisplayTimeOffsetLong() throws IOException {
        writeCmd(G4RcvrCmd.READDISPLAYTIMEOFFSET);
        long result=0;
        result = BitTools.byteArraytoInt(readResponse());
        Log.d(TAG,"getDisplayTimeOffsetLong=>"+result);
        return result;
    }

    protected long getSystemTimeOffsetLong() throws IOException {
        writeCmd(G4RcvrCmd.READSYSTEMTIMEOFFSET);
        long result=0;
        result = BitTools.byteArraytoInt(readResponse());
        Log.d(TAG,"getSystemTimeOffsetLong=>"+result);
        return result;
    }

    //@Override
    protected String getDatabasePartitionInfo() throws IOException {
        writeCmd(G4RcvrCmd.READDATABASEPARTITIONINFO);
        String  response = new String(readResponse());
        return response;
    }

    //CRC methods
    public static int calcCrc16 (byte [] buff) {
        int crc = 0;
        for (int i = 0; i < buff.length; i++)
        {
            crc = ((crc  >>> 8) | (crc  << 8) )& 0xffff;
            crc ^= (buff[i] & 0xff);
            crc ^= ((crc & 0xff) >> 4);
            crc ^= (crc << 12) & 0xffff;
            crc ^= ((crc & 0xFF) << 5) & 0xffff;

        }
        crc &= 0xffff;
        return crc;
    }

    public int writeCmd(G4RcvrCmd rcvrCmd, byte [] payload) throws IOException {
        Log.d(TAG,"Attempting to write to receiver");
        if(!isConnected()){
            Log.e(TAG,"Write failed - not connected to device");
            return 0;
        }
        int bytesWritten=0;
        if (! cgmTransport.isOpen())
            return bytesWritten;

        // Retrieve how many bytes should be sent by this command
        int bytesToWrite=rcvrCmd.getCmdSize();
        Log.d(TAG,"Bytes to write: "+bytesToWrite);
        int calcBytesToWrite=6;
        if (payload!=null)
            calcBytesToWrite = 4 + payload.length + 2;
//        Log.d(TAG,"Calculated bytes to write: "+calcBytesToWrite);
        if (bytesToWrite != calcBytesToWrite){
            Log.e(TAG,"Insufficient data for command");
            return 0;
        }
        //TODO throw an exception?
        if (bytesToWrite==-1){
            Log.e(TAG,"Command "+rcvrCmd.toString()+" has not been implemented");
            return bytesToWrite;
        }
        byte[] packet = new byte[bytesToWrite];

        packet[0]=0x01; // Always 1

        // next two bytes are the size of the command => SOF+packet size+command+payload+crc
        packet[1]=(byte) (bytesToWrite & 0xFF);
        packet[2]=(byte) (bytesToWrite >> 8 & 0xFF);
        packet[3]=rcvrCmd.getValue();

        // Copy the payload if it exists
        if (payload!=null && bytesToWrite>6 && payload.length>0)
            System.arraycopy(payload,0,packet,4,bytesToWrite-6);
        byte [] crcPacket=new byte[bytesToWrite-2];
        System.arraycopy(packet,0,crcPacket,0,bytesToWrite-2);
        int crc=calcCrc16(crcPacket);
        packet[bytesToWrite-2]=(byte)(crc & 255);
        packet[bytesToWrite-1]=(byte)(crc >> 8 & 255);

        try {
            Log.v(TAG,"Writing("+rcvrCmd.toString()+"): "+ BitTools.bytesToHex(packet));
            bytesWritten=cgmTransport.write(packet,G4Constants.defaultWriteTimeout);
            Log.d(TAG,"Bytes written - "+bytesWritten);
        } catch (IOException e) {
            throw new IOException("Unable to write to Dexcom G4");
//            Log.e(TAG, "Unable to write to Dexcom G4", e);

        }
        return bytesWritten;
    }


    public int writeCmd(G4RcvrCmd rcvCmd) throws IOException {
        return writeCmd(rcvCmd,null);
    }

    public byte[] readResponse(){
        return this.readResponse(G4Constants.defaultReadTimeout);
    }

    public byte[] readResponse(int millis) {
        Log.d(TAG,"Attempting to read to receiver");
        if(!isConnected()){
            Log.e(TAG,"Read failed - not connected to device");
            return null;
        }

        int bytesRead=0;
        if (! isConnected()) {
            Log.e(TAG,"Device is not connected");
            return null;
        }
        // Seems we can't read fewer bytes than they send..
        // but we can request more bytes than they'll send.
        // Setting max response buffer to 3072 to prevent
        // a read failure.
        // Max size required should be somewhere around 2122
        // while reading database pages. Let's just round
        // that up a bit
        byte [] responseBuffer = new byte[3072];

        try {
            bytesRead=cgmTransport.read(responseBuffer,millis);
            Log.d(TAG,"Bytes read - "+bytesRead);
        } catch (IOException e) {
            Log.e(TAG,"Unable to read headers from Dexcom G4");
            return null;
        }
        if (responseBuffer[0]!=0x01) {
            Log.e(TAG, "Unexpected response back while parsing header: "+ BitTools.bytesToHex(responseBuffer));
            return null;
        }
        int bytesToRead=(int)responseBuffer[2]<<8 ^ (int)responseBuffer[1];
        Log.d(TAG,"Calculated bytes to read at "+bytesRead);
        if (bytesToRead!=bytesRead) {
            Log.e(TAG, "Calculated bytes to read does not equal the bytes actually read");
            return null;
        }

        byte [] header = new byte[4];
        byte [] body = new byte[bytesRead-6];
        byte [] crc=new byte[2];

        System.arraycopy(responseBuffer,0,header,0,4);
        System.arraycopy(responseBuffer,4+(bytesToRead-6),crc,0,2);
        System.arraycopy(responseBuffer,4,body,0,bytesRead-6);

//        Log.d(TAG,"Response hex: " + BitTools.bytesToHex(header) + BitTools.bytesToHex(body) + BitTools.bytesToHex(crc));
        Log.v(TAG,"Header hex:"+ BitTools.bytesToHex(header));
        Log.v(TAG,"Body hex:"+ BitTools.bytesToHex(body));
        Log.v(TAG,"CRC hex: "+ BitTools.bytesToHex(crc));
        byte[] crcCheckArray=new byte[bytesToRead-2];
        System.arraycopy(responseBuffer,0,crcCheckArray,0,bytesToRead-2);
        int calcCRC=calcCrc16(crcCheckArray);
        int crcInt=(crc[0] & 0xFF);
        crcInt+=(crc[1] << 8) & 0xFF00;
        if (calcCRC!=crcInt) {
            Log.d(TAG, "Calculated CRC: " + calcCRC+"Response CRC: " + crcInt);
            Log.e(TAG, "CRC check failed!");
            return null;
        }else{
            Log.d(TAG,"Successful CRC check");
        }
        return body;
    }

    private G4DBPageHeader getPageHeader(G4RecType recType,int pageNumber) throws IOException {
        Log.d(TAG,"getPageHeader called");
        G4DBPageHeader result=new G4DBPageHeader();
        byte[] requestPayload=new byte[5];
        requestPayload[0]=recType.getValue();
        System.arraycopy(BitTools.intToByteArray(pageNumber),0,requestPayload,1,4);
        writeCmd(G4RcvrCmd.READDATABASEPAGEHEADER, requestPayload);
        byte[] resultBuffer=readResponse();
        byte[] FirstRecordIndexArray=new byte[4];
        byte[] NumberOfRecordsArray=new byte[4];
//        byte[] RecordTypeArray=new byte[1];
//        byte[] RevisionArray=new byte[1];
        byte[] PageNumberArray=new byte[4];
        byte[] Reserved2Array=new byte[4];
        byte[] Reserved3Array=new byte[4];
        byte[] Reserved4Array=new byte[4];
        byte[] CRCArray=new byte[2];

        System.arraycopy(resultBuffer,0,FirstRecordIndexArray,0,4);
        System.arraycopy(resultBuffer,4,NumberOfRecordsArray,0,4);
//        System.arraycopy(resultBuffer,8,RecordTypeArray,0,1);
//        System.arraycopy(resultBuffer,9,RevisionArray,0,1);
        System.arraycopy(resultBuffer,10,PageNumberArray,0,4);
        System.arraycopy(resultBuffer,14,Reserved2Array,0,4);
        System.arraycopy(resultBuffer,18,Reserved3Array,0,4);
        System.arraycopy(resultBuffer,22,Reserved4Array,0,4);
        System.arraycopy(resultBuffer,26,CRCArray,0,2);
        result.FirstRecordIndex= BitTools.byteArraytoInt(FirstRecordIndexArray);
        result.NumberOfRecords= BitTools.byteArraytoInt(NumberOfRecordsArray);
        result.RecordType=G4RecType.values()[(int) resultBuffer[8]];
        result.Revision=resultBuffer[9];
        result.PageNumber= BitTools.byteArraytoInt(PageNumberArray);
        result.Reserved2= BitTools.byteArraytoInt(Reserved2Array);
        result.Reserved3= BitTools.byteArraytoInt(Reserved3Array);
        result.Reserved4= BitTools.byteArraytoInt(Reserved4Array);
        int crcInt=(CRCArray[0] & 0xFF);
        crcInt+=(CRCArray[1] << 8) & 0xFF00;
        result.Crc=crcInt;
        Log.v(TAG,"FirstRecordIndex: "+result.FirstRecordIndex+" NumberOfRecords: "+result.NumberOfRecords+" RecordType: "+result.RecordType.toString()+" Revision: "+result.Revision+" PageNumber: "+result.PageNumber+"CRC: "+result.Crc);
        return result;
    }

    public G4DBPage[] getDBPages(G4RecType recType,int startPage, int numPages) throws IOException {
        Log.d(TAG,"Requesting "+numPages+" starting with "+startPage);
        byte [] requestPayload=new byte[6];
        requestPayload[0]=recType.getValue();
        requestPayload[1]=(byte) (startPage & 0xFF);
        requestPayload[2]=(byte) ((startPage >> 8) & 0xFF);
        requestPayload[3]=(byte) ((startPage >> 16) & 0xFF);
        requestPayload[4]=(byte) ((startPage >> 24) & 0xFF);
        requestPayload[5]=(byte) numPages;
        writeCmd(G4RcvrCmd.READDATABASEPAGES, requestPayload);
        byte[] response=readResponse();
        Log.d(TAG,"Response Length: "+response.length);
        G4DBPage[] pages=new G4DBPage[numPages];
        for (int i=0;i<numPages;i++){
            pages[i]=new G4DBPage();
            pages[i].PageHeader=getPageHeader(recType,startPage+i);
            int pgBoundary=528*(i+1);
            System.arraycopy(response,pgBoundary-500,pages[i].PageData,0,500);
            Log.v(TAG, "Page (" + i + "):" + BitTools.bytesToHex(pages[i].PageData));
            Log.v(TAG,"Page length ("+i+"):"+pages[i].PageData.length);
        }
        Log.d(TAG,"Returning "+pages.length+" pages from getDBPages");
        return pages;
    }

    // TODO this doesn't make sense. If this is a generic parse method, then why am I returning EGV results?
    public G4EGVRecord[] parsePage(G4DBPage page) {
//        Log.d(TAG,"Record type: "+page.PageHeader.RecordType.toString());
        if (page.PageHeader.RecordType==G4RecType.EGVDATA)
            return parseEGVPage(page);
        return null;
    }

    public G4EGVRecord[] parseEGVPage(G4DBPage page){
        G4EGVRecord[] results=new G4EGVRecord[page.PageHeader.NumberOfRecords];
        Log.d(TAG,"Processing "+page.PageHeader.NumberOfRecords+" records from page #"+page.PageHeader.PageNumber);
        for (int i=0;i<page.PageHeader.NumberOfRecords;i++){
            if (page.PageData.length>i*13){
                results[i]=new G4EGVRecord();
                byte[] recordBuffer=new byte[13];
                System.arraycopy(page.PageData, i * 13, recordBuffer, 0, 13);
                byte[] sysTimeArray=new byte[4];
                byte[] dispTimeArray=new byte[4];
                byte[] egvwflagArray=new byte[2];
                byte[] dirnoiseArray=new byte[1];
                byte[] crcArray=new byte[2];
                System.arraycopy(recordBuffer, 0, sysTimeArray, 0, 4);
                System.arraycopy(recordBuffer, 4, dispTimeArray, 0, 4);
                System.arraycopy(recordBuffer, 8, egvwflagArray, 0, 2);
                System.arraycopy(recordBuffer, 10, dirnoiseArray, 0, 1);
                System.arraycopy(recordBuffer, 11, crcArray, 0, 2);
                results[i].setSystemTime(BitTools.byteArraytoInt(sysTimeArray));
                long dtime=(long) BitTools.byteArraytoInt(dispTimeArray)*1000;
                Calendar mCalendar = new GregorianCalendar();
                TimeZone mTimeZone = mCalendar.getTimeZone();
//                long mGMTOffset = mTimeZone.getRawOffset();
//                long displayTimeLong=rcvrBaseDatems+dtime+mGMTOffset;
                long displayTimeLong=G4Constants.RECEIVERBASEDATE+dtime;
                if (mTimeZone.inDaylightTime(new Date())){
                    displayTimeLong-=3600000L;
                }
                Date displayTimeDate=new Date(displayTimeLong);

                results[i].setDate(displayTimeDate);
                int bgValue= BitTools.byteArraytoInt(egvwflagArray) & 0x3FF;
                // This means we've found the end
                if (bgValue==1023) {
                    Log.d(TAG,"Last reading found in this page");
                    break;
                }
                results[i].setEgv(bgValue);
                results[i].setSpecialValue(null);
//                results[i].s(deviceUnits);
                for (G4EGVSpecialValue e: G4EGVSpecialValue.values()) {
                    if (e.getValue()==bgValue) {
                        results[i].setSpecialValue(G4EGVSpecialValue.getEGVSpecialValue(bgValue));
                        Log.w(TAG,"Special value set: "+results[i].getSpecialValue().toString());
                        break;
                    }
                }
                int trendNoise= BitTools.byteArraytoInt(dirnoiseArray);
                results[i].setTrend(Trend.values()[trendNoise & 0xF]);
                results[i].setNoiseMode(G4NoiseMode.getNoiseMode((byte)(trendNoise & 0xF)>>4));
                Log.v(TAG,"Reading time("+i+"): "+results[i].getDate().toString()+" EGV: "+results[i].getEgv()+" Trend: "+results[i].getTrend().toString()+" Noise: "+results[i].getNoiseMode().toString());
            } else {
                Log.w(TAG,"Record ("+i+") appears to be truncated in page number "+page.PageHeader.PageNumber);
            }
        }
        Log.d(TAG,"Number of Records: "+results.length);
        return results;
    }

    public String getRcvrSerial() throws IOException {
        serialNum = getParam(G4RecType.MANUFACTURINGDATA, "SerialNumber");
        return serialNum;
    }

    private String getParam(G4RecType recType,String param) throws IOException {
        G4Partition part=getDBPageRange(recType);
        String result="";
//        Charset charset=
        G4DBPage[] pages=getDBPages(recType,part.firstPage,1);
        String data=new String();
        for (G4DBPage page:pages){
            int i;
            // Ugly code to capture the null terminated string
            for (i = 0; i < page.PageData.length && page.PageData[i] != 0x00; i++) { }
            // Strip the header and reduce the size of the string by the header length
            data+=new String(page.PageData,8,i-8);
        }
        try {
            InputStream is = new ByteArrayInputStream(data.getBytes("UTF-8"));
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
            DocumentBuilder builder=factory.newDocumentBuilder();
            Document dom=builder.parse(is);
            String elemName;
            if (recType==G4RecType.PCSOFTWAREPARAMETER) {
                elemName = "PCParameterRecord";
            }else if (recType==G4RecType.MANUFACTURINGDATA){
                elemName="ManufacturingParameters";
            } else {
                return "";
            }
            // TODO Need to add checking for null pointers...
            Element elem = (Element) dom.getElementsByTagName(elemName).item(0);
            result=elem.getAttribute(param);
        }catch (Exception e) {
            // TODO specific error handling
            Log.e(TAG,"Problem parsing XML from "+recType.toString()+" partition in getParam",e);
        }
        Log.d(TAG,recType.toString()+" data:"+data+"Result: "+result);
        return result;
    }

    public String getRcvrID() throws IOException {
        // Only get this value once per instance of a G4Device
        if (receiverID=="") {
            receiverID = getParam(G4RecType.PCSOFTWAREPARAMETER, "ReceiverId");
        } else {
            Log.d(TAG, "Returning cached results for ReceiverId");
        }
        String result=receiverID;
        return result;
    }

}
