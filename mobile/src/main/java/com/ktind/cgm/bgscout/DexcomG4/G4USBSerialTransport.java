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
 Copyright (c) 2014, Kevin Lee (klee24@gmail.com)
 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
 list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice, this
 list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

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