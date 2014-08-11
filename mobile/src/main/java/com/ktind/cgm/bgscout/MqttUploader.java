package com.ktind.cgm.bgscout;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

import com.ktind.cgm.bgscout.SGV;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by klee24 on 8/7/14.
 */
public class MqttUploader extends AbstractMonitor implements MqttCallback {
    private static final String TAG = AbstractMonitor.class.getSimpleName();
    protected String monitorType="MQTT Uploader";
    private static final String		MQTT_THREAD_NAME = "MqttService[" + TAG + "]"; // Handler Thread ID
    //    private static final String 	MQTT_BROKER = "iot.eclipse.org"; // Broker URL or IP Address
    private static final String MQTT_KEEP_ALIVE_TOPIC_FORMAT = "/users/%s/keepalive"; // Topic format for KeepAlives
    private static final String 	MQTT_BROKER = "192.168.1.2"; // Broker URL or IP Address

    private static final int 		MQTT_PORT = 1883;				// Broker Port

    public static final int			MQTT_QOS_0 = 0; // QOS Level 0 ( Delivery Once no confirmation )
    public static final int 		MQTT_QOS_1 = 1; // QOS Level 1 ( Delevery at least Once with confirmation )
    public static final int			MQTT_QOS_2 = 2; // QOS Level 2 ( Delivery only once with confirmation with handshake )

    private static final int 		MQTT_KEEP_ALIVE = 24000; // KeepAlive Interval in MS
    private static final String		MQTT_KEEP_ALIVE_TOPIC_FORAMT = "/users/%s/keepalive"; // Topic format for KeepAlives
    private static final byte[] 	MQTT_KEEP_ALIVE_MESSAGE = { 0 }; // Keep Alive message to send
    private static final int		MQTT_KEEP_ALIVE_QOS = MQTT_QOS_0; // Default Keepalive QOS

    private static final boolean 	MQTT_CLEAN_SESSION = true; // Start a clean session?

    private static final String 	MQTT_URL_FORMAT = "tcp://%s:%d"; // URL Format normally don't change

    private static final String 	ACTION_START 	= TAG + ".START"; // Action to start
    private static final String 	ACTION_STOP		= TAG + ".STOP"; // Action to stop
    private static final String 	ACTION_KEEPALIVE= TAG + ".KEEPALIVE"; // Action to keep alive used by alarm manager
    private static final String 	ACTION_RECONNECT= TAG + ".RECONNECT"; // Action to reconnect


    private static final String 	DEVICE_ID_FORMAT = "andr_%s"; // Device ID Format, add any prefix you'd like
    private String mDeviceId;		  // Device ID, Secure.ANDROID_ID
    private MqttDefaultFilePersistence mDataStore; // Defaults to FileStore
    private MemoryPersistence mMemStore; 		// On Fail reverts to MemoryStore
    private MqttConnectOptions mOpts;			// Connection Options

    protected MqttTopic mKeepAliveTopic;			// Instance Variable for Keepalive topic

    protected MqttClient mClient;					// Mqtt Client
    protected Handler mHandler=new Handler();
    protected AlarmReceiver alarmReceiver;
    protected NetworkConnectionIntentReceiver netConnReceiver;

    public MqttUploader(String n, int devID ,Context context) {
        super(n,devID,context);
//        appContext=context;
        this.setMonitorType("MQTT uploader");
        if (netConnReceiver == null){
            netConnReceiver = new NetworkConnectionIntentReceiver();
            appContext.registerReceiver(netConnReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        connect();
    }

    private void connect(){
        mDeviceId = String.format(DEVICE_ID_FORMAT,
                Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID));
//        mDataStore = new MqttDefaultFilePersistence(getAppContext().getCacheDir().getAbsolutePath());
        mMemStore = new MemoryPersistence();
        mOpts = new MqttConnectOptions();
        mOpts.setUserName("nsandroid");
        mOpts.setPassword("set4now".toCharArray());
        mOpts.setCleanSession(MQTT_CLEAN_SESSION);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
//        String url = String.format(Locale.US, MQTT_URL_FORMAT, MQTT_BROKER, MQTT_PORT);
        Log.d(TAG, "Connecting with URL: " + url);
        if (!url.equals("")) {
            try {
                mClient = new MqttClient(url, mDeviceId, mMemStore);
                mClient.connect(mOpts);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        } else {
            Log.w(TAG,"Unable to find MQTT URL for "+deviceIDStr);
        }
        AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        //TODO there has to be a better way to do this. I'm copy/pasting...
        Intent intent = new Intent("com.ktind.cgm.MQTT_KEEPALIVE");
        intent.putExtra("device",deviceIDStr);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(appContext, deviceID, intent, 0);
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // Set a repeating alarm to fire off an MQTT Keepalive for this device in 1 second and repeat it every 2.5 minutes
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis()+1000,150000,alarmIntent);

        alarmReceiver=new AlarmReceiver();
        appContext.registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.MQTT_KEEPALIVE"));
    }
    @Override
    protected void doProcess(DeviceDownloadObject d) {
        int index=d.getEgvRecords().length-1;
        int egv=d.getEgvRecords()[index].getEgv();
        Date date=d.getEgvRecords()[index].getDate();
        // TODO provide an iterator or something for egvrecords in DeviceDownloadObject?
//      protobuf
//        SGV.Practical8601 sgvMsg= SGV.Practical8601.newBuilder()
//                .setSgv(egv).setTimestamp(date.toString())
//                .setDirection(SGV.Practical8601.Direction.valueOf(d.getEgvRecords()[index].getTrend().getVal()))
//                .setDevice(SGV.Practical8601.Device.dexcom)
//                .build();
//        String msg=d.getEgvRecords()[d.getEgvRecords().length-1].getEgv()+":"+d.getEgvRecords()[d.getEgvRecords().length-1].getDate().getTime()+":"+d.getEgvRecords()[d.getEgvRecords().length-1].getTrend().getVal();
        try {
//            mClient.publish("/svgs",msg.getBytes(),MQTT_QOS_1,true);

            JSONObject json = new JSONObject();
            json.put("device", "dexcom");
            json.put("date", date.getTime());
            json.put("sgv", egv);
            json.put("direction", d.getEgvRecords()[index].getTrend().getNsString());
            String jsonString = json.toString();
            Log.i(TAG,"Publishing to entries/sgv: "+jsonString);
            mClient.publish("/entries/sgv",jsonString.getBytes(),MQTT_QOS_1,true);
        } catch (MqttException e) {
            Log.e(TAG,"Unable to publish message");
//            e.printStackTrace();
        } catch (JSONException e) {
            Log.e(TAG,"Unable to build JSON request",e);
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        Log.d(TAG,"Lost connection call back called");
        PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RemoteCGM");
        wl.acquire();
        if (isOnline()){
            mMemStore = new MemoryPersistence();
            mOpts = new MqttConnectOptions();
            // TODO Remove username/password
            mOpts.setUserName("nsandroid");
            mOpts.setPassword("set4now".toCharArray());
            mOpts.setCleanSession(MQTT_CLEAN_SESSION);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
            String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
//        String url = String.format(Locale.US, MQTT_URL_FORMAT, MQTT_BROKER, MQTT_PORT);
            Log.d(TAG, "Connecting with URL: " + url);
            if (!url.equals("")) {
                try {
                    mClient = new MqttClient(url, mDeviceId, mMemStore);
                    mClient.connect(mOpts);
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            } else {
                Log.w(TAG,"Unable to find MQTT URL for "+deviceIDStr);
            }
        } else {
            Log.e(TAG, "This device is not online");
        }
        wl.release();
    }

    @Override
    public void stop() {
        super.stop();
        try {
            mClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        if (alarmReceiver!=null)
            appContext.unregisterReceiver(alarmReceiver);
        if (netConnReceiver!=null)
            appContext.unregisterReceiver(netConnReceiver);
//        mHandler.removeCallbacks(startKeepAlives);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        Log.d(TAG,"Message arrived");

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.d(TAG,"Delivered message");
    }

    public void sendKeepalive() {
        Log.d(TAG, "Sending keepalive to " + MQTT_BROKER + " deviceID=>" + mDeviceId);
        MqttMessage message = new MqttMessage(MQTT_KEEP_ALIVE_MESSAGE);
        message.setQos(MQTT_KEEP_ALIVE_QOS);
        try {
            if (mKeepAliveTopic == null) {
                mKeepAliveTopic = mClient.getTopic(
                        String.format(Locale.US, MQTT_KEEP_ALIVE_TOPIC_FORMAT, mDeviceId));
                Log.d(TAG, "Topic set");
            }
            mKeepAliveTopic.publish(message);
            mClient.setCallback(MqttUploader.this);
        } catch (MqttException e) {
            Log.d(TAG, "Ran into an exception " + e.toString());
//            connect();
            // TODO separate the connect to its own. The alarms were going haywire..
            mMemStore = new MemoryPersistence();
            mOpts = new MqttConnectOptions();
            mOpts.setUserName("nsandroid");
            mOpts.setPassword("set4now".toCharArray());
            mOpts.setCleanSession(MQTT_CLEAN_SESSION);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
            String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
//        String url = String.format(Locale.US, MQTT_URL_FORMAT, MQTT_BROKER, MQTT_PORT);
            Log.d(TAG, "Connecting with URL: " + url);
            if (!url.equals("")) {
                try {
                    mClient = new MqttClient(url, mDeviceId, mMemStore);
                    mClient.connect(mOpts);
                } catch (MqttException ee) {
                    ee.printStackTrace();
                }
            } else {
                Log.w(TAG,"Unable to find MQTT URL for "+deviceIDStr);
            }
            e.printStackTrace();
        }
    }


    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.ktind.cgm.MQTT_KEEPALIVE")){
                if (intent.getExtras().get("device").equals(deviceIDStr)) {
                    Log.d(TAG, "Received a request to perform an MQTT keepalive operation on " + intent.getExtras().get("device"));
                    sendKeepalive();
                }else{
                    Log.d(TAG,deviceIDStr+": Ignored a request for "+intent.getExtras().get("device")+" to perform an MQTT keepalive operation");
                }
            }
        }
    }

    // FIXME - Copy/Paste... ugh
    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected());
    }

    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RemoteCGM");
            wl.acquire();
            if (isOnline()){
                Log.i(TAG, "Connection online. Attempting to reconnect");
                // TODO separate the connect to its own. The alarms were going haywire..
                mMemStore = new MemoryPersistence();
                mOpts = new MqttConnectOptions();
                mOpts.setUserName("nsandroid");
                mOpts.setPassword("set4now".toCharArray());
                mOpts.setCleanSession(MQTT_CLEAN_SESSION);
                SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
                String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
//        String url = String.format(Locale.US, MQTT_URL_FORMAT, MQTT_BROKER, MQTT_PORT);
                Log.d(TAG, "Connecting with URL: " + url);
                if (!url.equals("")) {
                    try {
                        mClient = new MqttClient(url, mDeviceId, mMemStore);
                        mClient.connect(mOpts);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.w(TAG,"Unable to find MQTT URL for "+deviceIDStr);
                }
            }
            wl.release();
        }
    }

    // TODO honor disable background data setting..

}
