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

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.ArrayList;

/**
 * Created by klee24 on 9/7/14.
 */
public class NotifHelper {
    static private ArrayList<String> messages=new ArrayList<String>();

    static public void notify(Context context,String message){
        messages.add(message);
        notify(context,messages);
    }

    static public void clearMessage(Context context,String msg){
        if (messages.contains(msg))
            messages.remove(msg);
        else
            return;
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (messages.size()==0)
            mNotifyMgr.cancel(153);
        else {
            notify(context,messages);
        }

    }

    static private void notify(Context context,ArrayList<String> msgs){
        String msg="";
        for (String m:msgs){
            if (!msg.equals(""))
                msg+="\n";
            msg+=m;
        }
        NotificationManager mNotifyMgr = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Notification.Builder notifBuilder=new Notification.Builder(context);
        Bitmap bm = BitmapFactory.decodeResource(context.getResources(), R.drawable.exclamationmarkicon);
        notifBuilder.setPriority(Notification.PRIORITY_HIGH)
                .setContentTitle("Sugarcast Errors")
                .setDefaults(Notification.DEFAULT_ALL)
                .setTicker(msg)
                .setContentText(msg)
                .setLargeIcon(bm)
                .setSmallIcon(R.drawable.exclamationmarkicon);
        Notification notification=notifBuilder.build();
        mNotifyMgr.notify(153,notification);
    }

}
