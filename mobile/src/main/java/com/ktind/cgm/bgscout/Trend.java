package com.ktind.cgm.bgscout;

/**
 * Created by klee24 on 8/2/14.
 */
public enum Trend {
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
    private Trend(String strVal, int integerVal,String nsStr){
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

    public Trend getTrendByNsString(String search){
        for (Trend t:values()){
            if (t.getNsString().equals(search))
                return t;
        }
        return Trend.NONE;
    }
}
