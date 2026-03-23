package com.retailone.pos.utils;

public class OtherUtils {

    public static String byteToHexString( byte[] b,int length) {
        String a = "";
        for (int i = 0; i < length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            a = a+hex;
        }
        return a;
    }

    public static long bytesToLong(byte[] bytes,int offset)
    {
        long l64;
        l64=0l;
        l64 =  (long)bytes[offset]&0xff;
        l64 |= (long)(bytes[offset + 2]&0xff)<<8;
        l64 |= (long)(bytes[offset + 4]&0xff)<<16;
        l64 |= (long)(bytes[offset + 6]&0xff)<<24;
        l64 |= (long)(bytes[offset + 8]&0xff)<<32;
        l64 |= (long)(bytes[offset + 10]&0xff)<<40;
        l64 |= (long)(bytes[offset + 12]&0xff)<<48;
        l64 |= (long)(bytes[offset + 14]&0xff)<<56;
        return l64;
    }
}
