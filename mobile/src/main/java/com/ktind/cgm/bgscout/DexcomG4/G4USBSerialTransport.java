package com.ktind.cgm.bgscout.DexcomG4;

import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.ktind.cgm.bgscout.CGMTransportAbstract;
import com.ktind.cgm.bgscout.NoDeviceFoundException;
import com.ktind.cgm.bgscout.USB.USBPower;
import com.ktind.cgm.bgscout.USB.UsbSerialDriver;
import com.ktind.cgm.bgscout.USB.UsbSerialProber;

import java.io.IOException;

/**
 * Created by klee24 on 7/21/14.
 */
public class G4USBSerialTransport extends CGMTransportAbstract {
    private static final String TAG = G4USBSerialTransport.class.getSimpleName();
    //    private UsbManager mUsbManager;
    private UsbSerialDriver mSerialDevice;
    Context appContext;
    int totalBytesRead=0;
    int totalBytesWritten=0;
//    boolean chargeReceiver=false;


    public G4USBSerialTransport(Context c){
        appContext=c;
    }

    @Override
    public boolean open() throws NoDeviceFoundException {
        USBPower.PowerOn();
        if (! isOpen()){
            Log.v(TAG, "Attempting to connect");
            UsbManager mUsbManager;
            mUsbManager=(UsbManager) appContext.getSystemService(Context.USB_SERVICE);
            mSerialDevice = UsbSerialProber.acquire(mUsbManager);
            if (mSerialDevice != null) {
                try {
                    mSerialDevice.open();
                    Log.d(TAG, "Successfully connected");
                    isopen = true;
                } catch (IOException e) {
//                    Log.e(TAG, "Unable to establish a serial connection to Dexcom G4");
                    isopen = false;
                    throw new NoDeviceFoundException("Unable to establish a serial connection to Dexcom G4");
                }
            }else{
//                Log.e(TAG,"Unable to acquire USB Manager");
                throw new NoDeviceFoundException("Unable to acquire USB manager");
            }
        }else{
            Log.w(TAG, "Already connected");
        }

        return false;
    }
//    public void setChargeReceiver(boolean charge){
//        chargeReceiver=charge;
//    }

    @Override
    public void close() {
        if (isOpen() && mSerialDevice!=null) {
            if (!chargeDevice) {
                Log.v(TAG,"chargeReceiver:"+chargeDevice);
                Log.d(TAG,"Disabling USB power");
                USBPower.PowerOff();
            }
            Log.v(TAG, "Attempting to disconnect");
            try {
                mSerialDevice.close();
                Log.d(TAG, "Successfully disconnected");
                isopen = false;
            } catch (IOException e) {
                Log.e(TAG, "Unable to close serial connection to Dexcom G4");
            }
        } else {
            Log.w(TAG,"Already disconnected");
        }
    }

    @Override
    public int read(byte[] responseBuffer,int timeoutMillis) throws IOException {
        int bytesRead;
        bytesRead=mSerialDevice.read(responseBuffer,timeoutMillis);
        totalBytesRead+=bytesRead;
        return bytesRead;
    }

    @Override
    public int write(byte [] packet, int writeTimeout) throws IOException {
        int bytesWritten;
        bytesWritten=mSerialDevice.write(packet, writeTimeout);
        totalBytesWritten+=bytesWritten;
        return bytesWritten;
    }
}