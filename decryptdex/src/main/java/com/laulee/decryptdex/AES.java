package com.laulee.decryptdex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by laulee
 * on 2020-02-17.
 */
public class AES {

    private static String skey = "aadsghzxcxhaghxe";

    /**
     * 配置ciper
     *
     * @param mode
     * @return
     */
    private static Cipher initCipher(int mode) {
        Cipher cipher = null;
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(skey.getBytes(), "AES");
            cipher = Cipher.getInstance("AES");
            cipher.init(mode, secretKeySpec);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return cipher;
    }

    /**
     * 加密流
     *
     * @param bytes
     * @return
     */
    public static byte[] encrypt(byte[] bytes) {
        Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
        try {
            return cipher.doFinal(bytes);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    /**
     * 解密流
     *
     * @param bytes
     * @return
     */
    public static byte[] decrypt(byte[] bytes) {
        Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
        try {
            return cipher.doFinal(bytes);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    /**
     * 获取bytes
     *
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
     * 解密文件并重新写入
     *
     * @param file
     */
    public static void decrypt(File file) {
        byte[] bytes = null;
        try {
            bytes = getBytes(file);
            byte[] decryptBytes = decrypt(bytes);
            //将加密后的dex，写入到原dex
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(decryptBytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
