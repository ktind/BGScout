package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public enum GlucoseUnit {
    NONE("None",(byte) 0),
    MGDL("mg/dL",(byte) 1),
    MMOL("mmol/L",(byte) 2);


    private String unit;
    private byte value;
    private GlucoseUnit(String mUnit, byte mVal){
        unit=mUnit;
        value=mVal;
    }

    public byte getValue() {
        return value;
    }

    @Override
    public String toString() {
        return unit;
    }
}