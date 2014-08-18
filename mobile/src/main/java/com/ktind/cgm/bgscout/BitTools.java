package com.ktind.cgm.bgscout;

//import android.util.Log;

/**
 * Created by klee24 on 7/20/14.
 */
public class BitTools {
    private static final String TAG = BitTools.class.getSimpleName();
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public static int byteArraytoInt(byte[] b){
//        if (b == null || b.length<4)
        if (b == null )
            return -1;
        int val=0;
        int counter=0;
        for (byte v: b){
            val+=(v & 0x000000FF) << counter*8;
            counter+=1;
        }
        return val;
    }



    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = hexArray[v >>> 4];
            hexChars[j * 3 + 1] = hexArray[v & 0x0F];
            hexChars[j * 3 + 2] = " ".toCharArray()[0];
        }
        return new String(hexChars);
    }

    public static byte[] intToByteArray(int i){
        byte[] byteArray=new byte[4];
        byteArray[0]=(byte) (i & 0xFF);
        byteArray[1]=(byte) ((i >> 8) & 0xFF);
        byteArray[2]=(byte) ((i >> 16) & 0xFF);
        byteArray[3]=(byte) ((i >> 24) & 0xFF);
        return byteArray;
    }

}