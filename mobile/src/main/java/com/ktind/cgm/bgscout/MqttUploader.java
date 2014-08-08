package com.ktind.cgm.bgscout;

import android.content.Context;
import android.os.Handler;
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
import com.ktind.cgm.bgscout.SGV;

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

    private MqttTopic mKeepAliveTopic;			// Instance Variable for Keepalive topic

    private MqttClient mClient;					// Mqtt Client
    private Handler mHandler=new Handler();
    private Context appContext;

    public MqttUploader(String n, Context context) {
        super(n);
        appContext=context;
        connect();
    }

    public Context getAppContext() {
        return appContext;
    }

    private void connect(){
        mDeviceId = String.format(DEVICE_ID_FORMAT,
                Settings.Secure.getString(getAppContext().getContentResolver(), Settings.Secure.ANDROID_ID));
        mDataStore = new MqttDefaultFilePersistence(getAppContext().getCacheDir().getAbsolutePath());
        mOpts = new MqttConnectOptions();
        mOpts.setCleanSession(MQTT_CLEAN_SESSION);
        String url = String.format(Locale.US, MQTT_URL_FORMAT, MQTT_BROKER, MQTT_PORT);
        Log.d(TAG, "Connecting with URL: " + url);
        try {
            mClient=new MqttClient(url,mDeviceId,mDataStore);
            mClient.connect(mOpts);
        } catch (MqttException e) {
            e.printStackTrace();
        }
        mHandler.post(startKeepAlives);
    }
    @Override
    protected void doProcess(DeviceDownloadObject d) {
        int index=d.getEgvRecords().length-1;
        int egv=d.getEgvRecords()[index].getEgv();
        Date date=d.getEgvRecords()[index].getDate();

        String msg=d.getEgvRecords()[d.getEgvRecords().length-1].getEgv()+":"+d.getEgvRecords()[d.getEgvRecords().length-1].getDate().getTime()+":"+d.getEgvRecords()[d.getEgvRecords().length-1].getTrend().getVal();
        try {
            mClient.publish("/svgs",msg.getBytes(),MQTT_QOS_1,true);
        } catch (MqttException e) {
            Log.e(TAG,"Issue publishing svgs msg: "+msg);
            e.printStackTrace();
        }
    }

    private Runnable startKeepAlives=new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"Sending keepalive to "+MQTT_BROKER+" deviceID=>"+mDeviceId);
            MqttMessage message=new MqttMessage(MQTT_KEEP_ALIVE_MESSAGE);
            message.setQos(MQTT_KEEP_ALIVE_QOS);
            try {
                if(mKeepAliveTopic == null) {
                    Log.d(TAG,"Setting topic?");
                    mKeepAliveTopic = mClient.getTopic(
                            String.format(Locale.US, MQTT_KEEP_ALIVE_TOPIC_FORAMT,mDeviceId));
                }
                mKeepAliveTopic.publish(message);
                mClient.setCallback(MqttUploader.this);
            } catch (MqttException e) {
                Log.d(TAG,"Ran into an exception "+e.toString());
                e.printStackTrace();
            }
            mHandler.postDelayed(startKeepAlives,MQTT_KEEP_ALIVE);
        }
    };


    @Override
    public void connectionLost(Throwable throwable) {
        Log.w(TAG,"Lost connection");
    }

    @Override
    public void stop() {
        super.stop();
        mHandler.removeCallbacks(startKeepAlives);
    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        Log.d(TAG,"Message arrived");

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
        Log.d(TAG,"Delivered message");
    }
}
