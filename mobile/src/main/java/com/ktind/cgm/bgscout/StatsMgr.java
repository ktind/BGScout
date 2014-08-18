package com.ktind.cgm.bgscout;

import java.util.ArrayList;


/**
 * Created by klee24 on 8/15/14.
 */
public class StatsMgr implements StatsInterface {
    protected ArrayList<StatsInterface> collectors=new ArrayList<StatsInterface>();

    public synchronized void registerCollector(StatsInterface si){
        collectors.add(si);
    }

    public synchronized void unregisterCollector(StatsInterface si){
        collectors.remove(si);
    }

    @Override
    public synchronized void logStats() {
        for (StatsInterface collector:collectors){
            collector.logStats();
        }
    }

    @Override
    public synchronized void reset() {
        for (StatsInterface collector:collectors){
            collector.reset();
        }
    }
}
