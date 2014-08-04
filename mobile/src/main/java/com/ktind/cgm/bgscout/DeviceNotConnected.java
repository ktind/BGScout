package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/3/14.
 */
public class DeviceNotConnected extends Exception{
    public DeviceNotConnected(){}

    public DeviceNotConnected(String message){
        super(message);
    }

    public DeviceNotConnected(String message, Throwable e){
        super(message,e);
    }

}
