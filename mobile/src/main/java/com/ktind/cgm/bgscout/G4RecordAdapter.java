package com.ktind.cgm.bgscout;

import com.ktind.cgm.bgscout.DexcomG4.G4EGVRecord;

import java.util.ArrayList;
import java.util.Arrays;

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
