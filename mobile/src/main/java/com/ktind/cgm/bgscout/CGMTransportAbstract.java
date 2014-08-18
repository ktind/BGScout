package com.ktind.cgm.bgscout;

import android.util.Log;

import java.io.IOException;


/**
 * Created by klee24 on 8/2/14.
 */
abstract public class CGMTransportAbstract {
    protected boolean isopen=false;
    protected boolean chargeDevice=false;

    abstract public boolean open() throws NoDeviceFoundException;
    abstract public void close();
    abstract public int read(byte[] responseBuffer,int timeoutMillis) throws IOException;
    abstract public int write(byte[] packet,int timeoutMillis) throws IOException;

    public void setChargeReceiver(boolean c){
        Log.d("G4Device", "Abstract setting charge receiver to " + c);
        chargeDevice=c;
    }

    public boolean isOpen(){
        return isopen;
    }
}
