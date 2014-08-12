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
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;

/**
 * Created by klee24 on 8/11/14.
 */

public class MQTTMgr implements MqttCallback,MQTTMgrObservable {
    private ArrayList<MQTTMgrObserverInterface> observers=new ArrayList<MQTTMgrObserverInterface>();

    private static final String TAG = MQTTMgr.class.getSimpleName();
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

    private AlarmReceiver alarmReceiver;
    private String mDeviceId;
    //    private MqttDefaultFilePersistence mDataStore;
    private MemoryPersistence mDataStore; 		// On Fail reverts to MemoryStore
    private MqttConnectOptions mOpts;			// Connection Options
    private MqttTopic mKeepAliveTopic;			// Instance Variable for Keepalive topic
    private MqttClient mClient;					// Mqtt Client
    private NetworkConnectionIntentReceiver netConnReceiver;
    private Context appContext;
    private String user=null;
    private String pass=null;
    private String mqttUrl=null;
    private String[] mqTopics=null;
//    private boolean mqConnected=false;

    public MQTTMgr(Context appContext, String user, String pass) {
        super();
        this.appContext=appContext;
        this.user=user;
        this.pass=pass;
    }

    public void connect(String url) {
        if (user==null || pass==null){
            Log.e(TAG,"User and/or password is null. Please verify arguments to the constructor");
            return;
        }
        // Save the URL for later re-connections
        mqttUrl=url;
        mDeviceId = String.format(DEVICE_ID_FORMAT,
                Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID));
        mDataStore = new MemoryPersistence();
        mOpts = new MqttConnectOptions();
        mOpts.setUserName(user);
        mOpts.setPassword(pass.toCharArray());
//        mOpts.setUserName("nsandroid");
//        mOpts.setPassword("set4now".toCharArray());
        mOpts.setCleanSession(MQTT_CLEAN_SESSION);
        Log.d(TAG, "Connecting to URL: " + url);
        try {
            mClient = new MqttClient(url, mDeviceId, mDataStore);
            mClient.connect(mOpts);
//            mqConnected=true;
//            mClient.subscribe("entries/sgv");
        } catch (MqttException e) {
            Log.e(TAG, "Error while connecting: ", e);
            //TODO possibly throw an exception?
        }
        appContext.registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.MQTT_KEEPALIVE"));
        AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        //TODO change intent identifier to something unique
        Intent intent = new Intent("com.ktind.cgm.MQTT_KEEPALIVE");
//        intent.putExtra("device",deviceIDStr);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // Set a repeating alarm to fire off an MQTT Keepalive for this device in 1 second and repeat it every 2.5 minutes
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis()+1000,150000,alarmIntent);

    }

    //FIXME this means that all classes Mgrs will be required to have some unique identifer
    public class AlarmReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.ktind.cgm.MQTT_KEEPALIVE")){
//                if (intent.getExtras().get("device").equals(deviceIDStr)) {
//                    Log.d(TAG, "Received a to perform an MQTT keepalive operation on " + intent.getExtras().get("device"));
                sendKeepalive();
//                }else{
//                    Log.d(TAG,deviceIDStr+": Ignored a request for "+intent.getExtras().get("device")+" to perform an MQTT keepalive operation");
//                }
            }
        }
    }


    public void subscribe(String... topics){
        mqTopics=topics;
        for (String topic: mqTopics){
            try {
                Log.d(TAG,"Subscribing to "+topic);
                mClient.subscribe(topic);
                mClient.setCallback(MQTTMgr.this);
            } catch (MqttException e) {
                Log.e(TAG, "Unable to subscribe to topic "+topic,e);
            }
        }
    }

    public void publish(String message,String topic){
        try {
            mClient.publish(topic,message.getBytes(),MQTT_QOS_1,true);
        } catch (MqttException e) {
            Log.e(TAG,"Unable to publish message: "+message+" to "+topic);
        }
//        mClient.publish("/entries/sgv",jsonString.getBytes(),MQTT_QOS_1,true);
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null &&
                cm.getActiveNetworkInfo().isAvailable() &&
                cm.getActiveNetworkInfo().isConnected());
    }

    @Override
    public void registerObserver(MQTTMgrObserverInterface observer) {
        observers.add(observer);
    }

    @Override
    public void unregisterObserver(MQTTMgrObserverInterface observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String... messages) {
        for (MQTTMgrObserverInterface observer:observers){
            Log.v(TAG,"Calling back to registered users");
            observer.onMessage(messages);
        }

    }

    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (mqttUrl==null || mqTopics==null){
                Log.e(TAG,"It appears that the connection URL and/or Topics have not been set previously and I've received a networkconnectionintention. Possibly never used the connect() and/or subscribe() methods");
                return;
            }
            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RemoteCGM");
            wl.acquire();
            if (isOnline()){
                Log.i(TAG, "Connection online. Attempting to reconnect");
                connect(mqttUrl);
                subscribe(mqTopics);
            }
            wl.release();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        Log.d(TAG,"Lost connection call back called");
        if (mqttUrl==null || mqTopics==null){
            Log.e(TAG,"Somehow lost the connection and mqttUrl and/or mqTopics have not been set. Make sure to use connect() and subscribe() methods of this class");
            return;
        }
        PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RemoteCGM");
        wl.acquire();
        if (isOnline()) {
            connect(mqttUrl);
            subscribe(mqTopics);
        }
        wl.release();
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        byte[] message=mqttMessage.getPayload();
        Log.i(TAG,"  Topic:\t" + s +
                "  Message:\t" + new String(message) +
                "  QoS:\t" + mqttMessage.getQos());
        notifyObservers(new String(message));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.i(TAG,"deliveryComplete called");
    }

    public void sendKeepalive() {
        Log.d(TAG, "Sending keepalive to " + MQTT_BROKER + " deviceID=>" + mDeviceId);
        if (mqTopics==null || mqttUrl==null){
            Log.e(TAG,"URL and/or Topics have not been set as expected. Ignoring sendKeepalive command until these values are set");
            return;
        }
        MqttMessage message = new MqttMessage(MQTT_KEEP_ALIVE_MESSAGE);
        message.setQos(MQTT_KEEP_ALIVE_QOS);
        try {
            if (mKeepAliveTopic == null) {
                Log.v(TAG, "Setting topic");
                mKeepAliveTopic = mClient.getTopic(
                        String.format(Locale.US, MQTT_KEEP_ALIVE_TOPIC_FORMAT, mDeviceId));
            }
            mKeepAliveTopic.publish(message);
            mClient.setCallback(MQTTMgr.this);
        } catch (MqttException e) {
            connect(mqttUrl);
            subscribe(mqTopics);
        }
    }

    public void disconnect(){
        try {
            mClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
    // TODO honor disable background data setting..

}
