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

import android.content.Context;
import android.util.Log;

import com.ktind.cgm.bgscout.model.DownloadDataSource;

import java.sql.SQLException;

/**
 * Created by klee24 on 8/30/14.
 */
public class SQLiteMonitor extends AbstractMonitor{
    private static final String TAG = MainActivity.class.getSimpleName();

    public SQLiteMonitor(String n, int devID, Context context) {
        super(n, devID, context, "sqlite_monitor");
    }

    @Override
    protected void doProcess(DownloadObject d) {
        DownloadDataSource downloadDataSource=new DownloadDataSource(context);
        try {
            downloadDataSource.open();
            for (EGVRecord egvRecord:d.getEgvArrayListRecords()) {
                long epoch=egvRecord.getDate().getTime();
                int trend=egvRecord.getTrend().getVal();
                int unit=egvRecord.getUnit().getValue();
                int egv=egvRecord.getEgv();
                downloadDataSource.createEGV(epoch,deviceIDStr,egv,unit,trend);
            }
            // FIXME - using the time from the last record for now but needs to change to d.getDownloadDate.getTime() once the uploader starts sending it =)
            downloadDataSource.createBattery(d.getDeviceBattery(),deviceIDStr,"cgm",d.getLastRecordReadingDate().getTime());
            downloadDataSource.createBattery(d.getUploaderBattery(),deviceIDStr,"uploader",d.getLastRecordReadingDate().getTime());
            // TODO add battery for this device?
            downloadDataSource.close();
            savelastSuccessDate(d.getLastRecordReadingDate().getTime());
        } catch (SQLException e) {
            Log.e(TAG,"Caught SQLException: ",e);
        } catch (NoDataException e) {
            Log.e(TAG, "No data in download. Unable to save last reading date");
        }
    }
}
