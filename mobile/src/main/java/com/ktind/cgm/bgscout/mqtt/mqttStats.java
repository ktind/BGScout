package com.ktind.cgm.bgscout.mqtt;

import android.util.Log;

import com.ktind.cgm.bgscout.StatsInterface;

import java.util.Date;
import java.util.HashMap;

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
public class mqttStats implements StatsInterface {
    private static final String TAG = mqttStats.class.getSimpleName();
    private int connects=0;
    private int reconnects=0;
    private int disconnects=0;
    private int keepalives=0;
    private int lostConnections=0;
    private int delivered=0;
    private HashMap<String,Integer> publishes=new HashMap<String, Integer>();
    private HashMap<String,Integer> messages=new HashMap<String, Integer>();
    private Date startTime;
    private Date stopTime;
    private int totalMessages;
    private int totalPublishes;
    private int networkNotifications;


    public void start(){
        startTime=new Date();
    }

    public void stop(){
        stopTime=new Date();
    }

    public void reset(){
        connects=0;
        reconnects=0;
        disconnects=0;
        keepalives=0;
        lostConnections=0;
        delivered=0;
        publishes=new HashMap<String, Integer>();
        messages=new HashMap<String, Integer>();
        startTime=null;
        stopTime=null;
        totalMessages=0;
        totalPublishes=0;
        networkNotifications=0;
    }

    public long getDurationMillis(){
        Date endTime;
        if (stopTime!=null){
            endTime=stopTime;
        }
        endTime=new Date();
        if (startTime!=null)
            return startTime.getTime()-endTime.getTime();
        return 0;
    }

    public void addNetworkNotification(){
        networkNotifications+=1;
    }

    public void addConnect(){
        connects+=1;
    }

    public void addReconnect(){
        reconnects+=1;
        connects-=1;
    }

    public void addDisconnect(){
        disconnects+=1;
    }

    public void addPublish(String topic){
        int val=0;
        if (messages.containsKey(topic))
            val=publishes.get(topic);
        val+=1;
        publishes.put(topic,val+1);
        totalPublishes+=1;
    }

    public void addMessage(String topic){
        int val=0;
        if (messages.containsKey(topic))
            val=messages.get(topic);
        val+=1;
        messages.put(topic,val);
        totalMessages+=1;
    }

    public void addKeepAlive(){
        keepalives+=1;
    }

    public void addLostConnections(){
        lostConnections+=1;
    }

    public void addDelivered(){
        delivered+=1;
    }

    public int getConnects() {
        return connects;
    }

    public int getReconnects() {
        return reconnects;
    }

    public int getDisconnects() {
        return disconnects;
    }

    public int getKeepalives() {
        return keepalives;
    }

    public int getPublishes(String topic) {
        return publishes.get(topic);
    }

    public int getMessages(String topic) {
        return messages.get(topic);
    }

    public int getTotalMessages() {
        return totalMessages;
    }

    public int getTotalPublishes() {
        return totalPublishes;
    }

    public int getNetworkNotifications() {
        return networkNotifications;
    }

    public int getLostConnections() {
        return lostConnections;
    }

    public int getDelivered() {
        return delivered;
    }

    public void logStats(){
        Log.d(TAG, "Connects: "+connects);
        Log.d(TAG, "Lost connections: "+lostConnections);
        Log.d(TAG, "Re-connects: "+reconnects);
        Log.d(TAG, "Disconnects: "+disconnects);
        Log.d(TAG, "Keep-alives: "+keepalives);
        Log.d(TAG, "Total messages published: "+totalPublishes);
        for (String key:publishes.keySet()){
            Log.d(TAG,"Outgoing ("+key+"): "+publishes.get(key));
        }
        Log.d(TAG, "Total messages received: "+totalMessages);
        for (String key:messages.keySet()){
            Log.d(TAG,"Incoming ("+key+"): "+messages.get(key));
        }
        Log.d(TAG,"Network notifications: "+networkNotifications);

        Log.d(TAG, "Duration: "+(getDurationMillis()/1000)/60+" minutes");
    }
}
