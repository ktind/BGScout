package com.ktind.cgm.bgscout.mqtt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.ktind.cgm.bgscout.BGScout;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by klee24 on 8/11/14.
 */

public class MQTTMgr implements MqttCallback,MQTTMgrObservable {
    private ArrayList<MQTTMgrObserverInterface> observers=new ArrayList<MQTTMgrObserverInterface>();

    private static final String TAG = MQTTMgr.class.getSimpleName();
    public static final int	MQTT_QOS_0 = 0; // QOS Level 0 ( Delivery Once no confirmation )
    public static final int MQTT_QOS_1 = 1; // QOS Level 1 ( Delevery at least Once with confirmation )
    public static final int	MQTT_QOS_2 = 2; // QOS Level 2 ( Delivery only once with confirmation with handshake )
    private static final int MQTT_KEEP_ALIVE = 24000; // KeepAlive Interval in MS
    private static final String MQTT_KEEP_ALIVE_TOPIC_FORMAT = "/users/%s/keepalive"; // Topic format for KeepAlives
    private static final byte[] MQTT_KEEP_ALIVE_MESSAGE = { 0 }; // Keep Alive message to send
    private static final int MQTT_KEEP_ALIVE_QOS = MQTT_QOS_0; // Default Keepalive QOS
    private static final boolean MQTT_CLEAN_SESSION = false;
    private static final String MQTT_URL_FORMAT = "tcp://%s:%d";
    private static final String DEVICE_ID_FORMAT = "android1_%s";

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
    private String lastWill=null;
    private String deviceIDStr =null;
    public mqttStats stats=new mqttStats();

    public MQTTMgr(Context appContext, String user, String pass, String deviceIDStr) {
        super();
        this.appContext=appContext;
        this.user=user;
        this.pass=pass;
        this.deviceIDStr=deviceIDStr;
        mDeviceId = String.format(DEVICE_ID_FORMAT,
                Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID));
        BGScout.statsMgr.registerCollector(stats);
    }

    public void connect(String url){
        connect(url,null);
    }

    public void connect(String url, String lwt) {
        if (user == null || pass == null) {
            Log.e(TAG, "User and/or password is null. Please verify arguments to the constructor");
            return;
        }
        stats.addConnect();
        setupOpts(lwt);
//        if (lwt != null)
//            lastWill = lwt;
//        mOpts = new MqttConnectOptions();
//        mOpts.setUserName(user);
//        Log.d(TAG, "Current keepalive is: " + mOpts.getKeepAliveInterval());
//        mOpts.setPassword(pass.toCharArray());
//        mOpts.setKeepAliveInterval(150);
//        if (lastWill != null) {
//            mOpts.setWill("/uploader", lastWill.getBytes(), 2, true);
//        }
//        mOpts.setCleanSession(MQTT_CLEAN_SESSION);

        // Save the URL for later re-connections
        mqttUrl = url;
        mDataStore = new MemoryPersistence();
        try {
            Log.d(TAG, "Connecting to URL: " + url);
            mClient = new MqttClient(url, mDeviceId, mDataStore);
            mClient.connect(mOpts);
        } catch (MqttException e) {
            Log.e(TAG, "Error while connecting: ", e);
        } finally {
            setupNetworkNotifications();
            setupKeepAlives();
        }
    }

    private void setupOpts(String lwt){
        if (lwt != null)
            lastWill = lwt;
        mOpts = new MqttConnectOptions();
        mOpts.setUserName(user);
        Log.d(TAG, "Current keepalive is: " + mOpts.getKeepAliveInterval());
        mOpts.setPassword(pass.toCharArray());
        mOpts.setKeepAliveInterval(150);
        if (lastWill != null) {
            mOpts.setWill("/uploader", lastWill.getBytes(), 2, true);
        }
        mOpts.setCleanSession(MQTT_CLEAN_SESSION);
    }

    private void setupKeepAlives(){
        Log.d(TAG,"Setting up keepalives");
        appContext.registerReceiver(alarmReceiver,new IntentFilter("com.ktind.cgm.MQTT_KEEPALIVE"));
        AlarmManager alarmMgr = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent("com.ktind.cgm.MQTT_KEEPALIVE");
        intent.putExtra("device",deviceIDStr);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(appContext, 0, intent, 0);
        Calendar calendar=Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // Set a repeating alarm to fire off an MQTT Keepalive for this device in 1 second and repeat it every 2.5 minutes
        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis()+1000,145000,alarmIntent);
        Log.d(TAG,"Finished setting up keepalives");
    }

    private void setupNetworkNotifications(){
        if (netConnReceiver == null)
        {
            netConnReceiver = new NetworkConnectionIntentReceiver();
            appContext.registerReceiver(netConnReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        }
    }

    protected class AlarmReceiver extends BroadcastReceiver {
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


    public void subscribe(String... topics){
        mqTopics=topics;
        Log.d(TAG,"Number of topics to subscribe to: "+mqTopics.length);
        for (String topic: mqTopics){
            try {
                Log.d(TAG, "Subscribing to " + topic);
                mClient.subscribe(topic, 2);
            } catch (MqttException e) {
                Log.e(TAG, "Unable to subscribe to topic "+topic,e);
            }
        }
        mClient.setCallback(MQTTMgr.this);
    }

    public void publish(String message,String topic){
        stats.addPublish(topic);
        Log.d(TAG,"Publishing "+message+" to "+ topic);
        try {
            mClient.publish(topic,message.getBytes(),2,true);
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
        Log.d(TAG,"Number of registered observers: "+observers.size());
    }

    @Override
    public void unregisterObserver(MQTTMgrObserverInterface observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String topic, MqttMessage message) {
        for (MQTTMgrObserverInterface observer:observers){
            Log.v(TAG,"Calling back to registered users");
            try {
                observer.onMessage(topic, message);
            } catch (Exception e){
                // Horrible catch all but I don't want the manager to die and reconnect
                Log.e(TAG,"Caught an exception: "+e.getMessage(),e);
            }
        }
    }

    protected class NetworkConnectionIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            if (mqttUrl==null || mqTopics==null){
                Log.e(TAG,"It appears that the connection URL and/or Topics have not been set previously and I've received a networkconnectionintention. Possibly never used the connect() and/or subscribe() methods");
                return;
            }
            stats.addNetworkNotification();
            PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RemoteCGM");
            wl.acquire();
            if (isOnline()){
                Log.i(TAG, "Network is online. Attempting to reconnect");
                reconnect();
            }
            wl.release();
        }
    }

    @Override
    public void connectionLost(Throwable throwable) {
        stats.addLostConnections();
        Log.w(TAG,"The connection was lost");
        if (mqttUrl==null || mqTopics==null){
            Log.e(TAG,"Somehow lost the connection and mqttUrl and/or mqTopics have not been set. Make sure to use connect() and subscribe() methods of this class");
            return;
        }
        PowerManager pm = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RemoteCGM");
        wl.acquire();
        if (isOnline())
            reconnect();
        wl.release();
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        Log.d(TAG,"Message arrived!");
        stats.addMessage(s);
        Log.d(TAG,"Before message payload");
        byte[] message=mqttMessage.getPayload();
        Log.d(TAG,"After message payload");
        Log.i(TAG,"  Topic:\t" + s +
                "  Message:\t" + new String(message) +
                "  QoS:\t" + mqttMessage.getQos());
        notifyObservers(s,mqttMessage);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.i(TAG,"deliveryComplete called");
        stats.addDelivered();
    }

    public void sendKeepalive() {
        stats.addKeepAlive();
        Log.d(TAG, "Sending keepalive to " + mqttUrl + " deviceID=>" + mDeviceId);
        MqttMessage message = new MqttMessage(MQTT_KEEP_ALIVE_MESSAGE);
        message.setQos(MQTT_KEEP_ALIVE_QOS);
        try {
            if (mKeepAliveTopic == null) {
                Log.v(TAG, "Setting topic");
                mKeepAliveTopic = mClient.getTopic(
                        String.format(Locale.US, MQTT_KEEP_ALIVE_TOPIC_FORMAT, mDeviceId));
            }
            mKeepAliveTopic.publish(message);
        } catch (MqttException e) {
            reconnect();
        }
    }

    public void reconnect(){
        stats.addReconnect();
        connect(mqttUrl);
        subscribe(mqTopics);
    }

    public void disconnect(){
        stats.addDisconnect();
        try {
            if (mClient!=null)
                mClient.disconnect();
        } catch (MqttException e) {
            Log.e(TAG,"Error disconnecting",e);
        }
        appContext.unregisterReceiver(netConnReceiver);
    }
    // TODO honor disable background data setting..

}
