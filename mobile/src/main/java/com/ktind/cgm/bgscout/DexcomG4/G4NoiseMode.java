package com.ktind.cgm.bgscout.DexcomG4;

/**
 * Created by klee24 on 7/20/14.
 */
public enum G4NoiseMode {
    None(0),
    Clean(1),
    Light(2),
    Medium(3),
    Heavy(4),
    NotComputed(5),
    Max(6);

    private int index;
    private G4NoiseMode(int i){
        index=i;
    }

    public int getValue(){
        return index;
    }

    public static G4NoiseMode getNoiseMode(int val){
        for (G4NoiseMode e: values()){
            if (e.getValue()==val)
                return e;
        }
        return null;
    }

}