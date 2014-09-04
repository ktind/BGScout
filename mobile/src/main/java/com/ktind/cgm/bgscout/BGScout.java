package com.ktind.cgm.bgscout;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

import org.acra.*;
import org.acra.annotation.*;
import org.acra.sender.HttpSender;

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

@ReportsCrashes(formKey = "",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text,
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://nightscout.iriscouch.com/acra-storage/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "scout",
        formUriBasicAuthPassword = "set4now",
        excludeMatchingSharedPreferencesKeys={".*mqtt_pass",".*nskey",".*nsapi",".*mongo_uri"}
)
public class BGScout extends Application {
    public static StatsMgr statsMgr=new StatsMgr();

    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);

    }

    synchronized public Tracker getTracker() {
        GoogleAnalytics analytics=GoogleAnalytics.getInstance(this);
        return analytics.newTracker(R.xml.app_tracker);
    }
}
