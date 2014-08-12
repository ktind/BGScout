package com.ktind.cgm.bgscout;

import android.app.Application;
import android.content.Intent;

import org.acra.*;
import org.acra.annotation.*;
import org.acra.sender.HttpSender;

/**
 * Created by klee24 on 8/11/14.
 */

@ReportsCrashes(formKey = "",
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text,
        httpMethod = HttpSender.Method.PUT,
        reportType = HttpSender.Type.JSON,
        formUri = "http://nightscout.iriscouch.com/acra-storage/_design/acra-storage/_update/report",
        formUriBasicAuthLogin = "scout",
        formUriBasicAuthPassword = "set4now"
)
public class BGScout extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ACRA.init(this);
    }
}
