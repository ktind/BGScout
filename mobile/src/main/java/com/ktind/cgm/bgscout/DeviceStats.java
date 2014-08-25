package com.ktind.cgm.bgscout;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;


/**
 Copyright (c) 2014, Kevin Lee (klee24@gmail.com)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice, this
 list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 */
public class DeviceStats implements StatsInterface{
    private static final String TAG = DeviceStats.class.getSimpleName();
    int connects=0;
    int disconnects=0;
    int downloads=0;
    int monitorFires=0;
    ArrayList<Long> downloadTimings=new ArrayList<Long>();
    ArrayList<Long> monitorTimings=new ArrayList<Long>();
    Date dlTimerStart;
    Date monitorTimerStart;

    public void addConnect(){
        connects+=1;
    }

    public void addDisconnect(){
        disconnects+=1;
    }

    public void addDownload(){
        downloads+=1;
    }

    public void addMonitorFire(){
        monitorFires+=1;
    }

    public void startDownloadTimer(){
        addDownload();
        dlTimerStart=new Date();
    }

    public void stopDownloadTimer(){
        if (dlTimerStart==null)
            return;
        Long timing=new Date().getTime()-dlTimerStart.getTime();
        if (timing<=0)
            return;
        downloadTimings.add(timing);
    }

    public void startMonitorTimer(){
        addMonitorFire();
        monitorTimerStart=new Date();
    }

    public void stopMonitorTimer(){
        if (monitorTimerStart==null)
            return;
        Long timing=new Date().getTime()-monitorTimerStart.getTime();
        if (timing<=0)
            return;
        monitorTimings.add(timing);
    }

    public void logStats(){
        Log.d(TAG, "Connects: "+connects);
        Log.d(TAG, "Disconnects: " + disconnects);
        Log.d(TAG, "Downloads: "+downloads);
        long total=0;
        int counter=1;
        for (Long timing:downloadTimings){
            Log.v(TAG,"Download timing #"+counter+": "+timing);
            total+=timing;
            counter+=1;
        }
        if (counter > 1)
            Log.d(TAG,"Average download time: "+((float)total/(float)counter));
        counter=1;
        for (Long timing:monitorTimings){
            Log.v(TAG,"Monitor timing #"+counter+": "+timing);
            total+=timing;
            counter+=1;
        }
        if (counter > 1)
            Log.d(TAG,"Average monitor time: "+((float)total/(float)counter));
    }

    @Override
    public void reset() {
        connects=0;
        disconnects=0;
        downloads=0;
        monitorFires=0;
        downloadTimings=new ArrayList<Long>();
        monitorTimings=new ArrayList<Long>();
        dlTimerStart=null;
        monitorTimerStart=null;
    }
}
