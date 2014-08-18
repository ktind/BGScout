package com.ktind.cgm.bgscout.DexcomG4;

import com.ktind.cgm.bgscout.Constants;

/**
 * Created by klee24 on 8/2/14.
 */
public class G4Constants extends Constants {
    final static int READING_INTERVAL= 60*5; // 5 minutes in seconds (60 seconds * 5 minutes)
    final static int defaultReadings=10;
    final static int defaultReadTimeout=200;
    final static int defaultWriteTimeout=200;
    final static long RECEIVERBASEDATE=1230789600000L;
}
