package com.ktind.cgm.bgscout.DexcomG4;

import java.util.ArrayList;

/**
 * Created by klee24 on 8/22/14.
 */
public interface G4RecordInterface {
    public ArrayList<? extends G4Record> parse(G4DBPage page);
}
