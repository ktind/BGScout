package com.ktind.cgm.bgscout.USB;

import android.os.Build;
import android.util.Log;

import java.io.DataOutputStream;

public class USBPower {

    private static final String TAG = USBPower.class.getSimpleName();

    private static final String SET_POWER_ON_COMMAND = "echo 'on' > \"/sys/bus/usb/devices/1-1/power/level\"";
    private static final String SET_POWER_SUSPEND_COMMAND_A = "echo \"0\" > \"/sys/bus/usb/devices/1-1/power/autosuspend\"";
    private static final String SET_POWER_SUSPEND_COMMAND_B = "echo \"auto\" > \"/sys/bus/usb/devices/1-1/power/level\"";
    private static final String SET_AUTHORIZED_ON_CMD = "echo 1 > /sys/bus/usb/devices/1-1/authorized";

    public static void PowerOff() {
        try {
            runCommand(SET_POWER_SUSPEND_COMMAND_A);
            runCommand(SET_POWER_SUSPEND_COMMAND_B);
            Log.i(TAG, "PowerOff USB complete");
        } catch (Exception e) {
            Log.e(TAG, "Unable to PowerOff USB");
        }
    }

    public static void PowerOn(){
        try {
            // Model specific command
            if (Build.MODEL.contains("ST18i")) {
                runCommand(SET_AUTHORIZED_ON_CMD);
            }
            runCommand(SET_POWER_ON_COMMAND);
            Log.i(TAG, "PowerOn USB complete");
        } catch (Exception e) {
            Log.e(TAG, "Unable to PowerOn USB");
        }
    }

    private static void runCommand(String command) throws Exception {
        Process process = Runtime.getRuntime().exec("su");
        DataOutputStream os = new DataOutputStream(process.getOutputStream());
        os.writeBytes(command + "\n");
        os.flush();
        os.writeBytes("exit \n");
        os.flush();
        os.close();
        process.waitFor();
    }
}
