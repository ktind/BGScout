/*
 * Copyright (c) 2014. , Kevin Lee (klee24@gmail.com)
 * All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without modification,
 *  are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice, this
 *  list of conditions and the following disclaimer in the documentation and/or
 *  other materials provided with the distribution.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ktind.cgm.bgscout;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

/**
 * Created by klee24 on 8/29/14.
 */
public class CGMDownloadObject extends DownloadObject {
    //TODO move stuff out of DownloadObject to here
    protected ArrayList<EGVRecord> egvRecords=new ArrayList<EGVRecord>();
    protected int deviceBattery;
    protected GlucoseUnit unit;

    public GlucoseUnit getUnit() {
        return unit;
    }

    public DownloadObject setUnit(GlucoseUnit unit) {
        this.unit = unit;
        return this;
    }

    public int getDeviceBattery() {
        return deviceBattery;
    }

    public DownloadObject setDeviceBattery(int deviceBattery) {
        this.deviceBattery = deviceBattery;
        return this;
    }

//    public ArrayList<EGVRecord> getEgvRecords() {
//        return egvRecords;
//    }

    public CGMDownloadObject setEgvRecords(ArrayList<EGVRecord> records) {
        this.egvRecords = records;
        return this;
    }

    public EGVRecord getLastRecord() throws NoDataException {
        if (egvRecords==null || egvRecords.size()==0)
            throw new NoDataException("There are no records in the download");
        return egvRecords.get(egvRecords.size()-1);
//        return egvRecords[egvRecords.length-1];
    }

    public Date getLastRecordReadingDate() throws NoDataException {
        return getLastRecord().getDate();
    }

    // FIXME is this really how it should work?
    public Date getLastReadingDate() {
        // First get shared preferences
        // Then check to see if there are any records
        // finally set to now - 2.5 hours.
        if (this.lastReadingDate!=null)
            return this.lastReadingDate;
        if (this.egvRecords != null && this.egvRecords.size() > 0)
            return egvRecords.get(egvRecords.size()-1).getDate();
//            return egvRecords[egvRecords.length-1].getDate();
        return new Date(new Date().getTime()-9000000L);
    }

    public void trimReadingsAfter(Long afterDateLong){
        // Create a new copy so that we don't ruin everyone's day by stomping on each others efforts.
        Log.d(TAG, "Size before trim: " + egvRecords.size());
        Date afterDate=new Date(afterDateLong);
        for (Iterator<EGVRecord> iterator = egvRecords.iterator(); iterator.hasNext(); ) {
            EGVRecord record = iterator.next();
            if (! record.getDate().after(afterDate))
                iterator.remove();
        }
        Log.d(TAG,"Size after trim: "+egvRecords.size());
    }

    public int getLastReading() throws NoDataException {
        return getLastRecord().getEgv();
    }

//    public String getLastReadingString(){
//        String result;
//        try {
//            return String.valueOf(getLastReading());
//        } catch (NoDataException e) {
//            return "---";
//        }
//    }

    public Trend getLastTrend() throws NoDataException {
        return getLastRecord().getTrend();
    }


}
