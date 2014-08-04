package com.ktind.cgm.bgscout;

import com.squareup.otto.Bus;

/**
 * Created by klee24 on 7/29/14.
 */
public class CGMBus {
    private static final Bus bus=new Bus();

    public static Bus getInstance(){
        return bus;
    }
}