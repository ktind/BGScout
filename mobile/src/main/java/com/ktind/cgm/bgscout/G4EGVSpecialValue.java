package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 7/20/14.
 */
public enum G4EGVSpecialValue {

    NONE("None",0),
    SENSORNOTACTIVE("Sensor not active",1),
    MINIMALLYEGVAB("Minimally EGV Aberration",2),
    NOANTENNA("No Antenna",3),
    SENSOROUTOFCAL("Sensor needs Calibration",5),
    COUNTSAB("Counts Aberration",6),
    ABSOLUTEAB("Absolute Aberration",9),
    POWERAB("Power Aberration",10),
    RFBADSTATUS("RF bad status",12);


    private String name;
    private int val;
    private G4EGVSpecialValue(String s, int i){
        name=s;
        val=i;
    }

    public int getValue(){
        return val;
    }

    public String toString(){
        return name;
    }

    public static G4EGVSpecialValue getEGVSpecialValue(int val){
        for (G4EGVSpecialValue e: values()){
            if (e.getValue()==val)
                return e;
        }
        return null;
    }
}