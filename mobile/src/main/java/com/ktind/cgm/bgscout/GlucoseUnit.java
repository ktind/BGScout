package com.ktind.cgm.bgscout;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

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

    public GlucoseUnit getUnitByString(String search){
        for (GlucoseUnit u:values()) {
            if (u.toString().equals(search)) {
                return u;
            }
        }
        return NONE;
    }

    public static class GlucoseUnitSerializer implements JsonSerializer<GlucoseUnit> {
        public JsonElement serialize(GlucoseUnit src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getValue());
        }
    }

    public static class GlucoseUnitDeSerializer implements JsonDeserializer<GlucoseUnit> {
        public GlucoseUnit deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return GlucoseUnit.valueOf(json.getAsJsonPrimitive().getAsString());
        }
    }
}