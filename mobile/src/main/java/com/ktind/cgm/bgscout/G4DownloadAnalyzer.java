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

import com.ktind.cgm.bgscout.DexcomG4.G4EGVSpecialValue;

/**
 * Created by klee24 on 8/28/14.
 */
public class G4DownloadAnalyzer extends CGMDownloadAnalyzer {
    protected final int MINEGV=39;
    protected final int MAXEGV=401;


    G4DownloadAnalyzer(DownloadObject dl, Context context) {
        super(dl,context);
    }

    @Override
    public AnalyzedDownload analyze() {
        try {
            super.analyze();
            checkSpecialValues();
        } catch (NoDataException e) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.WARN,"Download did not contain any data"),Conditions.NODATA);
            e.printStackTrace();
        }
        return this.downloadObject;
    }


    protected void checkSpecialValues() throws NoDataException {
        int egvValue=downloadObject.getLastReading();

        if (egvValue<MINEGV){
            G4EGVSpecialValue specialValue=G4EGVSpecialValue.getEGVSpecialValue(egvValue);
            AlertMessage alertMessage;
            if (specialValue!=null) {
                alertMessage=new AlertMessage(AlertLevels.WARN, specialValue.toString());
            }else{
                alertMessage=new AlertMessage(AlertLevels.WARN,"Unknown special value received from G4");
            }
            downloadObject.addMessage(alertMessage,Conditions.DEVICEMSGS);
        }
    }

    @Override
    protected void checkThresholdholds() throws NoDataException {
        int egv=downloadObject.getLastReading();
        if (egv>=MAXEGV) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "EGV is too high to read"),Conditions.CRITICALHIGH);
            return;
        } else if (egv<=MINEGV && ! G4EGVSpecialValue.isSpecialValue(egv)) {
            downloadObject.addMessage(new AlertMessage(AlertLevels.CRITICAL, "EGV is too low to read"),Conditions.CRITICALLOW);
            return;
        }
        super.checkThresholdholds();
    }
}
