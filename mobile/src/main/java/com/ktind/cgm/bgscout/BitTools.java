package com.ktind.cgm.bgscout;

//import android.util.Log;

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