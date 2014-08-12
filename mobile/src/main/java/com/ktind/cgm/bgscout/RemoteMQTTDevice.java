package com.ktind.cgm.bgscout;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.IOException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by klee24 on 8/6/14.
 */
public class RemoteMQTTDevice extends AbstractPushDevice implements MQTTMgrObserverInterface {
    private static final String TAG = RemoteMQTTDevice.class.getSimpleName();
//    private static final String 	MQTT_BROKER = "192.168.1.2";
//    private static final int MQTT_PORT = 1883;
//    public static final int	MQTT_QOS_0 = 0; // QOS Level 0 ( Delivery Once no confirmation )
//    public static final int MQTT_QOS_1 = 1; // QOS Level 1 ( Delevery at least Once with confirmation )
//    public static final int	MQTT_QOS_2 = 2; // QOS Level 2 ( Delivery only once with confirmation with handshake )
//    private static final int MQTT_KEEP_ALIVE = 24000; // KeepAlive Interval in MS
//    private static final String MQTT_KEEP_ALIVE_TOPIC_FORMAT = "/users/%s/keepalive"; // Topic format for KeepAlives
//    private static final byte[] MQTT_KEEP_ALIVE_MESSAGE = { 0 }; // Keep Alive message to send
//    private static final int MQTT_KEEP_ALIVE_QOS = MQTT_QOS_0; // Default Keepalive QOS
//    private static final boolean MQTT_CLEAN_SESSION = true;
//    private static final String MQTT_URL_FORMAT = "tcp://%s:%d";
//    private static final String DEVICE_ID_FORMAT = "android_%s";
//    private String mDeviceId;
//    private MqttDefaultFilePersistence mDataStore;
//    private MemoryPersistence mDataStore; 		// On Fail reverts to MemoryStore
//    private MqttConnectOptions mOpts;			// Connection Options
//    private MqttTopic mKeepAliveTopic;			// Instance Variable for Keepalive topic
//    private MqttClient mClient;					// Mqtt Client
    private MQTTMgr mqttMgr;

    public RemoteMQTTDevice(String n, int deviceID, Context appContext, Handler mH) {
        super(n, deviceID, appContext, mH);
        setDeviceType("Remote MQTT");
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
    }

    @Override
    public void connect() throws DeviceNotConnected {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(appContext);
        String url=sharedPref.getString(deviceIDStr+"_mqtt_endpoint","");
        // TODO Remove username/password
        mqttMgr=new MQTTMgr(appContext,"","");
        mqttMgr.connect(url);
        mqttMgr.registerObserver(this);
        mqttMgr.subscribe("entries/sgv");
    }

    @Override
    public void disconnect() {
        mqttMgr.disconnect();
        mqttMgr.unregisterObserver(this);
        mqttMgr=null;
    }

    @Override
    public void stop() {
        super.stop();
        disconnect();
    }

    @Override
    public void onMessage(String... messages) {
        EGVRecord[] recs = new EGVRecord[1];
        JSONObject jsonObject= null;
        int egv=-1;
        Date date=null;
        String textTrend=null;
        // FIXME these objects overwrite each other. Perhaps there is a better way to do this?
        for (String message: messages) {
            try {
                jsonObject = new JSONObject(message);
                egv = jsonObject.getInt("sgv");
                date = new Date(jsonObject.getLong("date"));
                textTrend = jsonObject.getString("direction");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Trend trend = Trend.NONE;
            for (Trend t : Trend.values()) {
                if (textTrend.equals(t.getNsString())) {
                    trend = t;
                    break;
                }
            }
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
            recs[0] = new EGVRecord(egv, date, trend, true);
            Log.d(TAG, "Record created");
            DeviceDownloadObject ddo = new DeviceDownloadObject(this, recs, DownloadStatus.SUCCESS);
            Log.d(TAG, "DownloadObject created");
            setLastDownloadObject(ddo);
            Log.d(TAG, "Firing monitors");
            fireMonitors();
            Log.d(TAG, "Monitors fired");
        }
        onDownload();
    }
}
