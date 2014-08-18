package com.ktind.cgm.bgscout.DexcomG4;

import com.ktind.cgm.bgscout.EGVRecord;

/**
 * Created by klee24 on 7/13/14.
 */
public class G4EGVRecord extends EGVRecord {;
    private long systemTime=0L;
    private G4EGVSpecialValue specialValue;
    private G4NoiseMode noiseMode;

    public void setSystemTime(long systemTime) {
        this.systemTime = systemTime;
    }

    public long getSystemTime() {
        return systemTime;
    }

    public void setSpecialValue(G4EGVSpecialValue specialValue) {
        this.specialValue = specialValue;
    }

    public G4EGVSpecialValue getSpecialValue() {
        return specialValue;
    }

    public void setNoiseMode(G4NoiseMode noiseMode) {
        this.noiseMode = noiseMode;
    }

    public G4NoiseMode getNoiseMode() {
        return noiseMode;
    }
}