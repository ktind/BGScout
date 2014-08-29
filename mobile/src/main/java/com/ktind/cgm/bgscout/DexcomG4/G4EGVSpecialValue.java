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

    public static boolean isSpecialValue(int val){
        for (G4EGVSpecialValue e: values()){
            if (e.getValue()==val)
                return true;
        }
        return false;
    }
}