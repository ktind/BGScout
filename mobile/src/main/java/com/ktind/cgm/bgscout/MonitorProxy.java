package com.ktind.cgm.bgscout;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by klee24 on 8/3/14.
 */
public class MonitorProxy extends AsyncTask<DeviceDownloadObject,Void,Void> {

    private ArrayList<AbstractMonitor> monitors;

    MonitorProxy(){

    }

    MonitorProxy(MonitorProxy mProxy){
        this.monitors=mProxy.monitors;
    }

    public void setMonitors(AbstractMonitor[] mons){
        ArrayList<AbstractMonitor> listMons=new ArrayList<AbstractMonitor>(Arrays.asList(mons));
        this.monitors=listMons;
    }
    public void setMonitors(ArrayList<AbstractMonitor> mons) {
        this.monitors = mons;
    }

    public void stopMonitors() {
        for (AbstractMonitor mon:monitors){
            mon.stop();
        }
    }

    @Override
    protected Void doInBackground(DeviceDownloadObject... dl) {
        for (AbstractMonitor mon: monitors){
            mon.process(dl[0]);
        }
        return null;
    }
}
