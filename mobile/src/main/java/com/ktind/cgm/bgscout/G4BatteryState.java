package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public enum G4BatteryState {
    Charging((byte) 1,"Charging"),
    NotCharging((byte) 2, "Not charging"),
    NTCFault((byte) 3,"NTCFault"),
    BadBattery((byte) 4, "Bad battery");

    private String stringValue=null;
    private byte byteVal;

    private G4BatteryState(byte b, String s){
        stringValue=s;
        byteVal=b;
    }

    public byte getValue(){
        return byteVal;
    }

    @Override
    public String toString(){
        return stringValue;
    }
}
