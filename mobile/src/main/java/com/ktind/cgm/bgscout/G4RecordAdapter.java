package com.ktind.cgm.bgscout;

import com.ktind.cgm.bgscout.DexcomG4.G4EGVRecord;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by klee24 on 8/22/14.
 */
public class G4RecordAdapter {
    static public EGVRecord[] convertToEGVRecordArray(G4EGVRecord... g4EGVRecords){
        ArrayList<EGVRecord> records=new ArrayList<EGVRecord>();
        for (G4EGVRecord g4rec:g4EGVRecords){
            EGVRecord record=new EGVRecord();
            record.setDate(g4rec.getDate());
            record.setEgv(g4rec.getEgv());
            Trend trend=Trend.values()[g4rec.getTrend().getVal()];
            record.setTrend(trend);
            records.add(record);
        }
        return records.toArray(new EGVRecord[records.size()]);
    }

    static public EGVRecord[] convertToEGVRecordArray(ArrayList<G4EGVRecord> g4EGVRecords){
        return convertToEGVRecordArray(g4EGVRecords.toArray(new G4EGVRecord[g4EGVRecords.size()]));
    }

    static public ArrayList<EGVRecord> convertToEGVRecordArrayList(ArrayList<G4EGVRecord> g4EGVRecords){
        return new ArrayList<EGVRecord>(Arrays.asList(convertToEGVRecordArray(g4EGVRecords.toArray(new G4EGVRecord[g4EGVRecords.size()]))));
    }

    static public ArrayList<EGVRecord> convertToEGVRecordArrayList(G4EGVRecord... g4EGVRecords){
        return new ArrayList<EGVRecord>(Arrays.asList(convertToEGVRecordArray(g4EGVRecords)));
    }
}
