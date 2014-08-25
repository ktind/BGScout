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
public enum G4Trend {
    NONE("None",0,"NONE"),
    DOUBLEUP("Double up",1,"DoubleUp"),
    SINGLEUP("Single up",2,"SingleUp"),
    FORTYFIVEUP("Forty-five up",3,"FortyFiveUp"),
    FLAT("Flat",4,"Flat"),
    FORTYFIVEDOWN("Forty-five down",5,"FortyFiveDown"),
    SINGLEDOWN("Single down",6,"SingleDown"),
    DOUBLEDOWN("Double down",7,"DoubleDown"),
    NOTCOMPUTE("Not computable",8,"NOT COMPUTABLE"),
    RATEOUTRANGE("Rate out of Range",9,"RATE OUT OF RANGE");

    private String stringVal;
    private int intVal;
    private String nsString="NONE";
    private G4Trend(String strVal, int integerVal, String nsStr){
        this.stringVal=strVal;
        this.intVal=integerVal;
        this.nsString=nsStr;
    }

    public int getVal() {
        return intVal;
    }

    @Override
    public String toString(){
        return stringVal;
    }

    public String getNsString(){
        return nsString;
    }

    public G4Trend getTrendByNsString(String search){
        for (G4Trend t:values()){
            if (t.getNsString().equals(search))
                return t;
        }
        return G4Trend.NONE;
    }
}
