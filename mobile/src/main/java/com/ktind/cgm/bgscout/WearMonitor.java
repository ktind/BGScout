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
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by klee24 on 9/13/14.
 */
public class WearMonitor extends AbstractMonitor implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = WearMonitor.class.getSimpleName();
    protected GoogleApiClient googleClient;
    private static final String MESSAGE_RECEIVED_PATH = "/entries/sgv";
    private static final String MESSAGE = "Hello wearable\n Via the data layer";
    private Context context;

    WearMonitor(String n,int id,Context cxt){
        super(n,id,cxt,"wearMonitor");
        googleClient = new GoogleApiClient.Builder(cxt)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        sendDataLayerMessage();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    protected void doProcess(DownloadObject d) {
        googleClient.connect();
    }

    @Override
    public void stop() {
        super.stop();
        if (null != googleClient && googleClient.isConnected()) {
            googleClient.disconnect();
        }
        super.stop();
    }

    private void sendDataLayerMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Get the connected nodes and wait for results
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(googleClient).await();
                for (Node node : nodes.getNodes()) {
                    // Send a message and wait for result
                    MessageApi.SendMessageResult result =
                            Wearable.MessageApi.sendMessage(googleClient, node.getId(),
                                    MESSAGE_RECEIVED_PATH, MESSAGE.getBytes()).await();
                    if (result.getStatus().isSuccess()) {
                        Log.v(TAG, "Message sent to : " + node.getDisplayName());
                    }
                    else {
                        // Log an error
                        Log.v(TAG, "MESSAGE ERROR: failed to send Message");
                    }
                }
            }
        }).start();
    }
}
