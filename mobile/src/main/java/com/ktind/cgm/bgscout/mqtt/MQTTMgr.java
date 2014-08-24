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
import com.ktind.cgm.bgscout.DownloadObject;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.ArrayList;
import java.util.Date;
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
    private static final long RECONNECT_DELAY=60000L;
    private static final int KEEPALIVE_INTERVAL=150000;

    private DownloadObject lastDownload;
    private AlarmReceiver alarmReceiver;
    private AlarmReceiver reconnectReceiver;
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
    public static final String RECONNECT_INTENT_FILTER="com.ktind.cgm.MQTT_RECONNECT";
    public static final String KEEPALIVE_INTENT_FILTER="com.ktind.cgm.MQTT_KEEPALIVE";
    private AlarmManager alarmMgr;
    private Intent reconnectIntent;
    private PendingIntent reconnectPendingIntent;
    private Intent keepAliveIntent;
    private PendingIntent keepAlivePendingIntent;
    protected boolean connected=false;
    protected boolean initialCallbackSetup=false;


    public MQTTMgr(Context appContext, String user, String pass, String deviceIDStr) {
        super();
        this.appContext=appContext;
        this.user=user;
        this.pass=pass;
        this.deviceIDStr=deviceIDStr;
        mDeviceId = String.format(DEVICE_ID_FORMAT,
                Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID));
        BGScout.statsMgr.registerCollector(stats);
        alarmMgr = (AlarmManager)appContext.getSystemService(Context.ALARM_SERVICE);
        reconnectReceiver=new AlarmReceiver();

        // Reconnect receiver setup
        reconnectIntent = new Intent(RECONNECT_INTENT_FILTER);
        reconnectIntent.putExtra("device",deviceIDStr);
        reconnectPendingIntent=PendingIntent.getBroadcast(appContext, 51, reconnectIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        appContext.registerReceiver(reconnectReceiver,new IntentFilter(RECONNECT_INTENT_FILTER));

//        // Keepalive receiver setup
//        keepAliveIntent = new Intent(KEEPALIVE_INTENT_FILTER);
//        keepAliveIntent.putExtra("device",deviceIDStr);
//        // TODO - See if FLAG_NO_CREATE will stomp on other instances
//        keepAlivePendingIntent=PendingIntent.getBroadcast(appContext, 61, keepAliveIntent, PendingIntent.FLAG_NO_CREATE);
//        appContext.registerReceiver(alarmReceiver,new IntentFilter(KEEPALIVE_INTENT_FILTER));
    }

    public void connect(String url){
        connect(url,null);
    }

    public void initConnect(String url){
        initConnect(url,null);
    }

    public void initConnect(String url, String lwt) {
        connect(url,lwt);
        setupNetworkNotifications();
        setupKeepAlives();
    }

    public void connect(String url, String lwt) {
        if (user == null || pass == null) {
            Log.e(TAG, "User and/or password is null. Please verify arguments to the constructor");
            return;
        }
//        mClient=null;
        stats.addConnect();
        setupOpts(lwt);
        // Save the URL for later re-connections
        mqttUrl = url;
        mDataStore = new MemoryPersistence();
        try {
            Log.d(TAG, "Connecting to URL: " + url);
            mClient = new MqttClient(url, mDeviceId, mDataStore);
            mClient.connect(mOpts);
            connected=true;
            setNextKeepAlive();
        } catch (MqttException e) {
            Log.e(TAG, "Error while connecting: ", e);
        }
    }

    private void setupOpts(String lwt){
        if (lwt != null)
            lastWill = lwt;
        mOpts = new MqttConnectOptions();
        mOpts.setUserName(user);
        mOpts.setPassword(pass.toCharArray());
        mOpts.setKeepAliveInterval(KEEPALIVE_INTERVAL);
        Log.d(TAG, "Current keepalive is: " + mOpts.getKeepAliveInterval());

//        if (lastWill != null) {
//            mOpts.setWill("/uploader", lastWill.getBytes(), 2, true);
//        }
        mOpts.setCleanSession(MQTT_CLEAN_SESSION);
    }

    private void setupKeepAlives(){
        Log.d(TAG, "Setting up keepalives");
        //FIXME - do we need two instances of AlarmReceiver or will one suffice?
        alarmReceiver=new AlarmReceiver();
        keepAliveIntent = new Intent(KEEPALIVE_INTENT_FILTER);
        keepAliveIntent.putExtra("device",deviceIDStr);
        keepAlivePendingIntent=PendingIntent.getBroadcast(appContext, 61, keepAliveIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        appContext.registerReceiver(alarmReceiver,new IntentFilter(KEEPALIVE_INTENT_FILTER));
//        appContext.registerReceiver(alarmReceiver,new IntentFilter(KEEPALIVE_INTENT_FILTER));

        // TODO - See if FLAG_NO_CREATE will stomp on other instances
        Log.d(TAG,"Setting up repeating alarm");
//        alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), KEEPALIVE_INTERVAL, keepAlivePendingIntent);
//        alarmMgr.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+3000L,keepAlivePendingIntent);

    }

    private void setupReconnect(){
        reconnectIntent = new Intent(RECONNECT_INTENT_FILTER);
        reconnectIntent.putExtra("device",deviceIDStr);
        reconnectPendingIntent=PendingIntent.getBroadcast(appContext, 61, reconnectIntent, PendingIntent.FLAG_NO_CREATE);
        appContext.registerReceiver(alarmReceiver,new IntentFilter(RECONNECT_INTENT_FILTER));
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
            Log.d(TAG,"Received a broadcast: "+intent.getAction());

            if (intent.getAction().equals(KEEPALIVE_INTENT_FILTER)){
                if (intent.getExtras().get("device").equals(deviceIDStr)) {
                    Log.d(TAG, "Received a request to perform an MQTT keepalive operation on " + intent.getExtras().get("device"));
                    Log.d(TAG,"EXTRA_ALARM_COUNT: "+intent.getStringExtra("EXTRA_ALARM_COUNT"));
                    sendKeepalive();
//                    alarmMgr.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+KEEPALIVE_INTERVAL-3000L,keepAlivePendingIntent);
                }else{
                    Log.d(TAG,deviceIDStr+": Ignored a request for "+intent.getExtras().get("device")+" to perform an MQTT keepalive operation");
                }
            } else if (intent.getAction().equals(RECONNECT_INTENT_FILTER)) {
                Log.d(TAG,"Received broadcast to reconnect");
                Log.d(TAG,"EXTRA_ALARM_COUNT: "+intent.getStringExtra("EXTRA_ALARM_COUNT"));
                reconnect();
            }
        }
    }

//    protected class KeepAliveReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Log.d(TAG,"Received a broadcast: "+intent.getAction());
//
//            if (intent.getAction().equals(KEEPALIVE_INTENT_FILTER)){
//                if (intent.getExtras().get("device").equals(deviceIDStr)) {
//                    Log.d(TAG, "Received a request to perform an MQTT keepalive operation on " + intent.getExtras().get("device"));
//                    Log.d(TAG,"EXTRA_ALARM_COUNT: "+intent.getStringExtra("EXTRA_ALARM_COUNT"));
//                    sendKeepalive();
//                }else{
//                    Log.d(TAG,deviceIDStr+": Ignored a request for "+intent.getExtras().get("device")+" to perform an MQTT keepalive operation");
//                }
//            } else if (intent.getAction().equals(RECONNECT_INTENT_FILTER)) {
//                Log.d(TAG,"Received broadcast to reconnect");
//                Log.d(TAG,"EXTRA_ALARM_COUNT: "+intent.getStringExtra("EXTRA_ALARM_COUNT"));
//                reconnect();
//            }
//        }
//    }

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
        Log.d(TAG,"Verifying callback setup");
        if (! initialCallbackSetup) {
            Log.d(TAG,"Setting up callback");
            mClient.setCallback(MQTTMgr.this);
            Log.d(TAG,"Set up callback");
            initialCallbackSetup=false;
        }
        Log.d(TAG,"Finished verifying callback setup");
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
            if (isOnline()) {
                if (mqttUrl==null || mqTopics==null){
                    Log.e(TAG,"It appears that the connection URL and/or Topics have not been set previously and I've received a networkconnectionintention. Possibly never used the connect() and/or subscribe() methods");
                    return;
                }
                stats.addNetworkNotification();
                if (isOnline()) {
                    Log.i(TAG, "Network is online. Attempting to reconnect");
                    connected=false;
                    reconnectDelayed(5000);
                }
//            wl.release();
            }
        }
    }

    protected boolean isConnected(){
        return connected;
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
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "RemoteCGM "+deviceIDStr);
        if (! mClient.isConnected()) {
            wl.acquire();
            connected=false;
            if (isOnline())
                reconnectDelayed();
            wl.release();
        } else {
            Log.wtf(TAG, "Received connection lost but mClient is reporting we're online?!");
        }
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        stats.addMessage(s);
        byte[] message=mqttMessage.getPayload();
        Log.i(TAG,"  Topic:\t" + s +
                "  Message:\t" + new String(message) +
                "  QoS:\t" + mqttMessage.getQos());
        if (!mqttMessage.isDuplicate())
            notifyObservers(s,mqttMessage);
        else
            Log.i(TAG, "Possible duplicate message");
        setNextKeepAlive();
    }

    private void setNextKeepAlive() {
        Log.d(TAG,"Canceling previous alarm");
        alarmMgr.cancel(keepAlivePendingIntent);
        Log.d(TAG,"Setting next keep alive to trigger in "+(KEEPALIVE_INTERVAL-3000)/1000+" seconds");
        alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + KEEPALIVE_INTERVAL - 3000, keepAlivePendingIntent);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.i(TAG,"deliveryComplete called");
        stats.addDelivered();
        setNextKeepAlive();
//        alarmMgr.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+KEEPALIVE_INTERVAL-3000,keepAlivePendingIntent);
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
//            setNextKeepAlive();
//            alarmMgr.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+KEEPALIVE_INTERVAL-3000,keepAlivePendingIntent);
        } catch (MqttException e) {
            Log.wtf(TAG,"Exception during ping",e);
            Log.wtf(TAG,"Reason code:"+e.getReasonCode());
            reconnectDelayed();
        }
    }

    public void reconnectDelayed(){
        reconnectDelayed(RECONNECT_DELAY);
    }

    public void reconnectDelayed(long delay_ms){
        Log.i(TAG, "Attempting to reconnect again in "+delay_ms/1000+" seconds");
        alarmMgr.set(AlarmManager.RTC_WAKEUP,new Date().getTime()+delay_ms,reconnectPendingIntent);
    }

    private void reconnect(){
        if (isOnline()) {
            Log.d(TAG, "Reconnecting");
            alarmMgr.cancel(reconnectPendingIntent);
            stats.addReconnect();
            mClient=null;
            mKeepAliveTopic=null;
            connect(mqttUrl);
            subscribe(mqTopics);
        } else {
            Log.d(TAG, "Reconnect requested but I was not online");
        }
    }


    public void disconnect(){
        stats.addDisconnect();
        try {
            if (mClient!=null)
                mClient.disconnect();
        } catch (MqttException e) {
            Log.e(TAG,"Error disconnecting",e);
        }
        if (appContext!=null && netConnReceiver !=null)
            appContext.unregisterReceiver(netConnReceiver);
        if (appContext!=null && alarmReceiver !=null)
            appContext.unregisterReceiver(alarmReceiver);
        if (appContext!=null && reconnectReceiver !=null)
            appContext.unregisterReceiver(reconnectReceiver);
        alarmMgr.cancel(keepAlivePendingIntent);
        alarmMgr.cancel(reconnectPendingIntent);

    }
    // TODO honor disable background data setting..

}
