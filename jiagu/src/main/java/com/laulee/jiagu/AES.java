package com.laulee.jiagu;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by laulee
 * on 2020-02-17.
 */
public class AES {

    private static String skey = "aadsghzxcxhaghxe";

    /**
     * 加密主classes.dex
     *
     * @param file
     */
    public static File encrypt2File(File file) {
        File encryptfile = null;
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            inputStream = new FileInputStream(file);
            encryptfile = new File(file.getParentFile().getPath() + "/dump"
                    + file.getName().substring(file.getName().indexOf(".")));
            if (!encryptfile.exists()) {
                encryptfile.createNewFile();
            }
            fileOutputStream = new FileOutputStream(encryptfile);
            Cipher cipher = initCipher(Cipher.ENCRYPT_MODE);
            CipherInputStream cipherInputStream = new CipherInputStream(inputStream, cipher);
            byte[] cache = new byte[1024];
            int length = 0;
            while ((length = cipherInputStream.read(cache)) != -1) {
                fileOutputStream.write(cache, 0, length);
                fileOutputStream.flush();
            }
            cipherInputStream.close();
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return encryptfile;
    }

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

    public static byte[] getBytes(File file) throws Exception {
        RandomAccessFile r = new RandomAccessFile(file, "r");
        byte[] buffer = new byte[(int) r.length()];
        r.readFully(buffer);
        r.close();
        return buffer;
    }

    /**
     * 加密文件并重新写入
     *
     * @param file
     */
    public static void encrypt(File file) {
        byte[] bytes = null;
        try {
            bytes = getBytes(file);
            byte[] encryptBytes = encrypt(bytes);
            //将加密后的dex，写入到原dex
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            fileOutputStream.write(encryptBytes);
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
