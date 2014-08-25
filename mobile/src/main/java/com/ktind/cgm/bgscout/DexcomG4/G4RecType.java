package com.ktind.cgm.bgscout.DexcomG4;

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