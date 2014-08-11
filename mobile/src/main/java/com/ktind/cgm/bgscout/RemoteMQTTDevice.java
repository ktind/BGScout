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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONObject;

/**
 * Created by klee24 on 8/6/14.
 */
public class RemoteMQTTDevice extends AbstractPushDevice implements MqttCallback {
//    mqtt://ctdsixni:Eb2jVZ_cvSmA@m10.cloudmqtt.com:12787
    private static final String TAG = RemoteMQTTDevice.class.getSimpleName();
    private static final String 	MQTT_BROKER = "192.168.1.2";
    private static final int MQTT_PORT = 1883;
    public static final int	MQTT_QOS_0 = 0; // QOS Level 0 ( Delivery Once no confirmation )
    public static final int MQTT_QOS_1 = 1; // QOS Level 1 ( Delevery at least Once with confirmation )
    public static final int	MQTT_QOS_2 = 2; // QOS Level 2 ( Delivery only once with confirmation with handshake )
    private static final int MQTT_KEEP_ALIVE = 24000; // KeepAlive Interval in MS
    private static final String MQTT_KEEP_ALIVE_TOPIC_FORMAT = "/users/%s/keepalive"; // Topic format for KeepAlives
    private static final byte[] MQTT_KEEP_ALIVE_MESSAGE = { 0 }; // Keep Alive message to send
    private static final int MQTT_KEEP_ALIVE_QOS = MQTT_QOS_0; // Default Keepalive QOS
    private static final boolean MQTT_CLEAN_SESSION = true;
    private static final String MQTT_URL_FORMAT = "tcp://%s:%d";
    private static final String DEVICE_ID_FORMAT = "android_%s";
    private String mDeviceId;
//    private MqttDefaultFilePersistence mDataStore;
    private MemoryPersistence mDataStore; 		// On Fail reverts to MemoryStore
    private MqttConnectOptions mOpts;			// Connection Options
    private MqttTopic mKeepAliveTopic;			// Instance Variable for Keepalive topic
    private MqttClient mClient;					// Mqtt Client
    private AlarmReceiver alarmReceiver;
    private NetworkConnectionIntentReceiver netConnReceiver;

    public RemoteMQTTDevice(String n, int deviceID, Context appContext, Handler mH) {
        super(n, deviceID, appContext, mH);
        setDeviceType("Remote MQTT");
//        super.setDeviceType("Remote MQTT");
        this.remote=true;
    }

    @Override
    public void onDataReady(DeviceDownloadObject ddo) {
    }

    @Override
    int getDeviceBattery() throws IOException {
        return 0;
    }

    @Override
    public void start() {
        super.start();
        try {
            connect();
        } catch (DeviceNotConnected deviceNotConnected) {
            deviceNotConnected.printStackTrace();
        }
        alarmReceiver=new AlarmReceiver();
        // Need to be careful here - it is possible that a monitor with connections to a different MQTT server could have the same Intent and ID...
        appContext.registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.MQTT_KEEPALIVE"));
        AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        //TODO there has to be a better way to do this. I'm copy/pasting...
        Intent intent = new Intent("com.ktind.cgm.MQTT_KEEPALIVE");
        intent.putExtra("device",deviceIDStr);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(appContext, deviceID, intent, 0);
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // Set a repeating alarm to fire off an MQTT Keepalive for this device in 1 second and repeat it every 2.5 minutes
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis()+1000,150000,alarmIntent);
//        if (netConnReceiver == null){
//            netConnReceiver = new NetworkConnectionIntentReceiver();
//            appContext.registerReceiver(netConnReceiver,
//                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
//        }
    }

    @Override
    public void connect() throws DeviceNotConnected {
        mDeviceId = String.format(DEVICE_ID_FORMAT,
                Settings.Secure.getString(getAppContext().getContentResolver(), Settings.Secure.ANDROID_ID));
        //mDataStore = new MemoryPersistence(getAppContext().getCacheDir().getAbsolutePath());
        mDataStore= new MemoryPersistence();
        mOpts = new MqttConnectOptions();
        // TODO Remove username/password
        mOpts.setUserName("nsandroid");
        mOpts.setPassword("set4now".toCharArray());
        mOpts.setCleanSession(MQTT_CLEAN_SESSION);
//        String url = String.format(Locale.US, MQTT_URL_FORMAT, MQTT_BROKER, MQTT_PORT);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
        Log.d(TAG, "Connecting with URL: " + url);
        if (!url.equals("")) {
            try {
                mClient = new MqttClient(url, mDeviceId, mDataStore);
                mClient.connect(mOpts);
                mClient.subscribe("entries/sgv");
            } catch (MqttException e) {
                Log.e(TAG, "Error while connecting: ", e);
                e.printStackTrace();
                throw new DeviceNotConnected();
            }
        } else {
            Log.w(TAG,"Unable to find MQTT URL for "+deviceIDStr);
        }
    }

    public void sendKeepalive() {
        Log.d(TAG, "Sending keepalive to " + MQTT_BROKER + " deviceID=>" + mDeviceId);
        MqttMessage message = new MqttMessage(MQTT_KEEP_ALIVE_MESSAGE);
        message.setQos(MQTT_KEEP_ALIVE_QOS);
        try {
            if (mKeepAliveTopic == null) {
                Log.d(TAG, "Setting topic");
                mKeepAliveTopic = mClient.getTopic(
                        String.format(Locale.US, MQTT_KEEP_ALIVE_TOPIC_FORMAT, mDeviceId));
                Log.d(TAG, "Topic set");
            }
            mKeepAliveTopic.publish(message);
            mClient.setCallback(RemoteMQTTDevice.this);
        } catch (MqttException e) {
            Log.d(TAG, "Ran into an exception " + e.toString());
            // TODO separate the connect to its own. The alarms were going haywire..
            mDataStore = new MemoryPersistence();
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
                    mClient = new MqttClient(url, mDeviceId, mDataStore);
                    mClient.connect(mOpts);
                    mClient.subscribe("entries/sgv");
                } catch (MqttException ee) {
                    ee.printStackTrace();
                }
            } else {
                Log.w(TAG,"Unable to find MQTT URL for "+deviceIDStr);
            }
            e.printStackTrace();
        }
    }


    @Override
    public void disconnect() {
        try {
            mClient.disconnect();
        } catch (MqttException e) {
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
            // TODO separate the connect to its own. The alarms were going haywire..
            mDataStore = new MemoryPersistence();
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
                    mClient = new MqttClient(url, mDeviceId, mDataStore);
                    mClient.connect(mOpts);
                    mClient.subscribe("entries/sgv");
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            } else {
                Log.w(TAG,"Unable to find MQTT URL for "+deviceIDStr);
            }
        } else {
            Log.e(TAG,"This device is not online");
        }
        wl.release();
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        byte[] egvByteArray=mqttMessage.getPayload();
        Log.i(TAG,"  Topic:\t" + s +
                "  Message:\t" + new String(egvByteArray) +
                "  QoS:\t" + mqttMessage.getQos());
        EGVRecord[] recs = new EGVRecord[1];
        JSONObject jsonObject=new JSONObject(new String(egvByteArray));
        int egv=jsonObject.getInt("sgv");
        Date date=new Date(jsonObject.getLong("date"));
        String textTrend=jsonObject.getString("direction");
        Trend trend=Trend.NONE;
        for (Trend t:Trend.values()){
            if (textTrend.equals(t.getNsString())){
                trend=t;
                break;
            }
        }
        // FIXME this should go up to onDataReady no?
//        SGV.Practical8601 sgv=SGV.Practical8601.parseFrom(egvByteArray);
//        int egv=sgv.getSgv();
//        Log.d(TAG,"EGV=>"+egv);
//        String dateStr=sgv.getTimestamp();
//        Log.d(TAG,"Date (String): "+dateStr);
//        //Fri Aug 08 08:31:56 CDT 2014
//        Date date =new SimpleDateFormat("ccc MMM dd HH:mm:ss z yyyy").parse(sgv.getTimestamp());
//        Log.d(TAG,"Date=>"+date);
//        Trend trend=Trend.values()[sgv.getDirection().getNumber()];
//        Log.d(TAG,"Trend=>"+trend.toString());
        recs[0]=new EGVRecord(egv,date,trend,true);
        Log.d(TAG,"Record created");
        DeviceDownloadObject ddo=new DeviceDownloadObject(this,recs,DownloadStatus.SUCCESS);
        Log.d(TAG,"DownloadObject created");
        setLastDownloadObject(ddo);
        Log.d(TAG,"Firing monitors");
        fireMonitors();
        Log.d(TAG,"Monitors fired");
        onDownload();
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.i(TAG,"deliveryComplete called");
    }

    @Override
    public void stop() {
        super.stop();
        disconnect();
        if (alarmReceiver!=null && appContext!=null)
            appContext.unregisterReceiver(alarmReceiver);
//        if (netConnReceiver!=null && appContext!=null){
//            appContext.unregisterReceiver(netConnReceiver);
//        }
    }

    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.ktind.cgm.MQTT_KEEPALIVE")){
                if (intent.getExtras().get("device").equals(deviceIDStr)) {
                    Log.d(TAG, "Received a to perform an MQTT keepalive operation on " + intent.getExtras().get("device"));
                    sendKeepalive();
                }else{
                    Log.d(TAG,deviceIDStr+": Ignored a request for "+intent.getExtras().get("device")+" to perform an MQTT keepalive operation");
                }
            }
        }
    }

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
                mDataStore = new MemoryPersistence();
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
                        mClient = new MqttClient(url, mDeviceId, mDataStore);
                        mClient.connect(mOpts);
                        mClient.subscribe("entries/sgv");
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
