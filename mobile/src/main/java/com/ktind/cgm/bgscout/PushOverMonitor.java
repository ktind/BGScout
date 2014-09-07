package com.ktind.cgm.bgscout;

import android.content.Context;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
public class PushOverMonitor extends AbstractMonitor {
    //    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
    private static final String TAG = PushOverMonitor.class.getSimpleName();
    private static final int SOCKET_TIMEOUT = 60 * 1000;
    private static final int CONNECTION_TIMEOUT = 30 * 1000;

    private DefaultHttpClient httpclient;

    PushOverMonitor(String n,int devID,Context context){
        super(n,devID,context,"pushover_uploader");
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT);
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        httpclient = new DefaultHttpClient(params);
//        setMonitorType("Pushover API");
        setAllowVirtual(true);
    }
    @Override
    protected void doProcess(DownloadObject d) {
//        String apiSecret="u8coc9VxGgUuNUDykh73zegsJHHenH";
        String user="u8coc9VxGgUuNUDykh73zegsJHHenH";
        String apiSecret="a6As5oZJhxLayHPKM2AGJSWG6VLMDk";
        String postURL="https://api.pushover.net/1/messages.json";
        Log.i(TAG, "Posting to: " + postURL);
        Log.d(TAG,"Number of records: "+d.getEgvArrayListRecords().size());
        for (EGVRecord record : d.getEgvArrayListRecords()) {
            try {
                HttpPost post = new HttpPost(postURL);
                final List<NameValuePair> nvps = new ArrayList<NameValuePair>();

                nvps.add(new BasicNameValuePair("token", apiSecret));
                nvps.add(new BasicNameValuePair("user", user));
                nvps.add(new BasicNameValuePair("message", "BG: "+record.getEgv()+" "+record.getDate() ));
                nvps.add(new BasicNameValuePair("title", "Sugarcast "+d.getDeviceName()));
                nvps.add(new BasicNameValuePair("url", "http://ktind-ns-mqtt.azurewebsites.net"));
//                String jsonString = "token=\"u8coc9VxGgUuNUDykh73zegsJHHenH\"";
//                jsonString+="user=melissabalandlee@gmail.com";
//                jsonString += "message=\"Sugarcast \n BG: "+record.getEgv()+" \n "+record.getDate()+"\"";
                post.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
//                StringEntity se = new StringEntity(jsonString);
//                post.setEntity(se);

                ResponseHandler responseHandler = new BasicResponseHandler();
                ByteArrayOutputStream os=new ByteArrayOutputStream();
                post.getEntity().writeTo(os);
                Log.d(TAG,"Post: "+post.getURI());
                for (Header header:post.getAllHeaders()){
                    Log.d(TAG,"Request Header: "+header);
                }
                Log.d(TAG,"Request Body: "+os.toString());

                String resp=(String) httpclient.execute(post, responseHandler);
                Log.d(TAG,"Response: "+resp);

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                // bail because the likelyhood of subsequent requests succeeding is low
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
