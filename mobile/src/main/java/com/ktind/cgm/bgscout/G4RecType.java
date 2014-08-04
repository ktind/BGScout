package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 7/13/14.
 */
public enum G4RecType {
    MANUFACTURINGDATA("ManufacturingData",(byte)0x00),
    FIRMWAREPARAMETERDATA("FirmwareParameterData",(byte)0x01),
    PCSOFTWAREPARAMETER("PCSoftwareParameter",(byte)0x02),
    SENSORDATA("SensorData",(byte)0x03),
    EGVDATA("EGVData",(byte)0x04),
    CALSET("CalSet",(byte)0x05),
    ABERRATION("Aberration",(byte)0x06),
    INSERTIONTIME("InsertionTime",(byte)0x07),
    RECEIVERLOGDATA("ReceiverLogData",(byte)0x08),
    RECEIVERERRORDATA("ReceiverErrorData",(byte)0x09),
    METERDATA("MeterData",(byte)0x0a),
    USEREVENTDATA("UserEventData",(byte)0x0b),
    USERSETTINGDATA("UserSettingData",(byte)0x0c),
    MAXVALUE("MaxValue",(byte)0x0d);

    private String stringValue;
    private byte byteVal;
    private G4RecType(String toString,byte value){
        stringValue=toString;
        byteVal=value;
    }

    public byte getValue(){
        return byteVal;
    }

    @Override
    public String toString(){
        return stringValue;
    }
}