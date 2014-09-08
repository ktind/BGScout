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

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by klee24 on 9/7/14.
 */
public class TimeTools {

    static public String getTimeDiffStr(Date start,Date end){
        long timeDiff = (int) (end.getTime()-start.getTime());
        String msg="~";
//        Log.d("XXX", "Start: " + start);
//        Log.d("XXX","End: "+end);
//        Log.d("XXX","Time difference: "+timeDiff);
        if (timeDiff<60000) {
            msg += "Now";
        }else if (timeDiff>60000 && timeDiff<3600000){
            msg += String.valueOf((timeDiff/1000)/60);
            msg += "m";
        }else if (timeDiff>3600000 && timeDiff<86400000){
            msg += String.valueOf(((timeDiff/1000)/60)/60);
            msg += "h";
        }else if (timeDiff>86400000 && timeDiff<604800000){
            msg += String.valueOf((((timeDiff/1000)/60)/60)/24);
            msg += "d";
        }else {
            msg=new SimpleDateFormat("HH:mm:ss MM/dd").format(start);
        }
        return msg;
    }
}
