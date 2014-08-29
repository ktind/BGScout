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

import java.util.ArrayList;

/**
 * Created by klee24 on 8/28/14.
 */
public class AnalyzedDownload extends DownloadObject {
    protected ArrayList<AlertMessage> messages=new ArrayList<AlertMessage>();
    protected boolean hasCritical=false;
    protected boolean hasInfo=false;
    protected boolean hasWarn=false;
    protected boolean criticalHigh=false;
    protected boolean criticalLow=false;
    protected boolean warnHigh=false;
    protected boolean warnLow=false;
    protected ArrayList<Conditions> conditions=new ArrayList<Conditions>();

    AnalyzedDownload(DownloadObject dl){
        super(dl);
    }

    public ArrayList<AlertMessage> getMessages() {
        return messages;
    }

    public void addMessage(AlertMessage message,Conditions condition){
        if (! conditions.contains(condition) && condition!=Conditions.NONE)
            conditions.add(condition);
        if (message.getAlertLevel()==AlertLevels.INFO)
            hasInfo=true;
        if (message.getAlertLevel()==AlertLevels.WARN)
            hasWarn=true;
        if (message.getAlertLevel()==AlertLevels.CRITICAL)
            hasCritical=true;
        this.messages.add(message);
    }

    public void setMessages(ArrayList<AlertMessage> messages) {
        this.messages = messages;
    }

    public boolean hasCritical() {
        return hasCritical;
    }

    public boolean hasInfo() {
        return hasInfo;
    }

    public boolean hasWarn() {
        return hasWarn;
    }

    public ArrayList<AlertMessage> getAlertsForLevel(AlertLevels alertLevel){
        ArrayList<AlertMessage> response=new ArrayList<AlertMessage>();
        for (AlertMessage message:messages){
            if (message.getAlertLevel()==alertLevel)
                response.add(message);
        }
        return response;
    }

    public boolean isCriticalHigh() {
        return criticalHigh;
    }

    public void setCriticalHigh(boolean criticalHigh) {
        this.criticalHigh = criticalHigh;
    }

    public boolean isCriticalLow() {
        return criticalLow;
    }

    public void setCriticalLow(boolean criticalLow) {
        this.criticalLow = criticalLow;
    }

    public boolean isWarnHigh() {
        return warnHigh;
    }

    public void setWarnHigh(boolean warnHigh) {
        this.warnHigh = warnHigh;
    }

    public boolean isWarnLow() {
        return warnLow;
    }

    public void setWarnLow(boolean warnLow) {
        this.warnLow = warnLow;
    }

    public ArrayList<Conditions> getConditions() {
        return conditions;
    }

    public void addCondition(Conditions condition) {
        this.conditions.add(condition);
    }
}
