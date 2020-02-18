package com.laulee.jiagu;

import java.io.File;
import java.io.RandomAccessFile;

/**
 * Created by laulee on 2020-02-18.
 */
public class Util {

    /**
     * @param file
     * @return
     * @throws
     */
    public static byte[] getBytes(File file) throws Exception {
        RandomAccessFile r = new RandomAccessFile(file, "r");
        byte[] buffer = new byte[(int) r.length()];
        r.readFully(buffer);
        r.close();
        return buffer;
    }

    /**
     * int 转 bytes
     *
     * @param n
     * @return
     */
    public static byte[] toBytes(int n) {
        byte[] b = new byte[4];
        b[3] = (byte) (n & 0xff);
        b[2] = (byte) (n >> 8 & 0xff);
        b[1] = (byte) (n >> 16 & 0xff);
        b[0] = (byte) (n >> 24 & 0xff);
        return b;
    }

    /**
     * byte[] 转 int
     *
     * @param b
     * @return
     */
    public static int toInt(byte[] b) {
        int res = 0;
        for (int i = 0; i < b.length; i++) {
            res += (b[i] & 0xff) << ((3 - i) * 8);
        }
        return res;
    }
}
