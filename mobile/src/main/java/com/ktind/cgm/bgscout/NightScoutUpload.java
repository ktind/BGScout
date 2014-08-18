package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
//import java.text.SimpleDateFormat;
//import java.util.Date;

/**
 * Created by klee24 on 8/10/14.
 */
public class NightScoutUpload extends AbstractMonitor {
//    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss aa");
    private static final String TAG = NightScoutUpload.class.getSimpleName();
    private static final int SOCKET_TIMEOUT = 60 * 1000;
    private static final int CONNECTION_TIMEOUT = 30 * 1000;
    private String apiSecret;
    private DefaultHttpClient httpclient;
    private SharedPreferences sharedPref;

    NightScoutUpload(String n,int devID,Context context){
        super(n,devID,context);
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setSoTimeout(params, SOCKET_TIMEOUT );
        HttpConnectionParams.setConnectionTimeout(params, CONNECTION_TIMEOUT);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        httpclient = new DefaultHttpClient(params);
        setMonitorType("Nightscout upload API");
        setAllowVirtual(false);
    }
    @Override
    protected void doProcess(DownloadObject d) {
        String postURL=sharedPref.getString(deviceIDStr+"_nsapi","");
        postURL += (postURL.endsWith("/") ? "" : "/") + "entries";
        Log.i(TAG, "Posting to: " + postURL);
        if (postURL=="") {
            Log.w(TAG,"Nightscout base API url is not set for "+getName()+"("+deviceIDStr+")");
            return;
        }
        int numRecs=d.getEgvRecords().length;
        int index=0;
        for (EGVRecord record : d.getEgvRecords()) {
            if (!record.isNew())
                continue;
            // hack to only send the last record for now...
            if (index < numRecs)
                continue;
            index+=1;
            try {
                HttpPost post = new HttpPost(postURL);
                JSONObject json = new JSONObject();
                json.put("device", "dexcom");
                json.put("date", record.getDate().getTime());
                json.put("sgv", record.getEgv());
                json.put("direction", record.getTrend().getNsString());
                String jsonString = json.toString();

                StringEntity se = new StringEntity(jsonString);
                post.setEntity(se);
                post.setHeader("Accept", "application/json");
                post.setHeader("Content-type", "application/json");
                apiSecret = sharedPref.getString(deviceIDStr+"_nskey","");
                MessageDigest md=MessageDigest.getInstance("SHA1");
                String apiSecretHash=byteArrayToHexString(md.digest(apiSecret.getBytes("UTF-8")));
                Log.d(TAG,"API Secret: "+apiSecretHash);
                post.setHeader("api-secret",apiSecretHash);
                /*
                POST /api/v1/entries/
                Accept: application/json
                Content-type: application/json
                api-secret: XXXXXX

                {"device":"dexcom","date":"1407695516000","sgv":"101","direction":"FortyFiveUp"}
                 */

                ResponseHandler responseHandler = new BasicResponseHandler();
                Log.d(TAG,"Post: "+post.getURI());

                String resp=(String) httpclient.execute(post, responseHandler);
                Log.d(TAG,"Response: "+resp);

            } catch (ClientProtocolException e) {
                e.printStackTrace();
                // bail because the likelyhood of subsequent requests succeeding is low
                break;
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                Log.d(TAG,"Unable to find SHA1 algorithm");
                e.printStackTrace();
                break;
            }
        }
    }

    protected static String byteArrayToHexString(byte[] b) {
        String result = "";
        for (int i=0; i < b.length; i++) {
            result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
        }
        return result;
    }
}
