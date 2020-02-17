package com.laulee.jiagu;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Created by laulee
 * on 2020-02-17.
 */
public class AES {

    private static String skey = "aadsghzxcxhaghxe";

    /**
     * dex加密
     *
     * @param targetDir
     */
    public static void encrypt(String targetDir) {
        File targetFile = new File(targetDir);
        if (targetFile.exists()) {
            File[] files = targetFile.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (file.exists() && !file.isDirectory()) {
                    String fileName = file.getName();
                    //过滤dex文件
                    if ("dex".equals(fileName.substring(fileName.lastIndexOf(".") + 1, fileName.length()))) {
                        File encryptFile = encrypt(file);
                        if (encryptFile != null) {

                        }
                    }
                }
            }
        }
    }

    /**
     * @param file
     */
    private static File encrypt(File file) {
        File encryptfile = null;
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            inputStream = new FileInputStream(file);
            encryptfile = new File(file.getParentFile().getPath() + "/secret-" + file.getName());
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
     * 解密
     *
     * @param targetDir
     */
    public static void decrypt(String targetDir) {
        File targetFile = new File(targetDir);
        if (targetFile.exists()) {
            File[] files = targetFile.listFiles();
            if (files == null) {
                return;
            }
            for (File file : files) {
                if (file.exists() && !file.isDirectory()) {
                    String fileName = file.getName();
                    //解密除去壳dex
                    if (fileName.endsWith(".dex") && !fileName.startsWith("classes.dex")) {
                        File decryptFile = decrypt(file);
                    }
                }
            }
        }
    }

    /**
     * 解密dex
     *
     * @param file
     * @return
     */
    private static File decrypt(File file) {
        String fileName = file.getName();
        File decryptFile = new File(file.getParent() + "/decrypt-" + fileName.substring(fileName.indexOf("-") + 1));
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        if (!decryptFile.exists()) {
            try {
                decryptFile.createNewFile();
                inputStream = new FileInputStream(file);
                fileOutputStream = new FileOutputStream(decryptFile);
                Cipher cipher = initCipher(Cipher.DECRYPT_MODE);
                CipherOutputStream cipherInputStream = new CipherOutputStream(fileOutputStream, cipher);
                int length = 0;
                byte[] bytes = new byte[1024];
                while ((length = inputStream.read(bytes)) != -1) {
                    cipherInputStream.write(bytes, 0, length);
                }
                cipherInputStream.close();
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
        }
        return decryptFile;
    }

    /**
     * 加密流
     *
     * @param bytes
     * @return
     */
    public static byte[] encrypt(byte[] bytes) {
        Cipher cipher = null;
        cipher = initCipher(Cipher.ENCRYPT_MODE);
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
        Cipher cipher = null;
        cipher = initCipher(Cipher.DECRYPT_MODE);
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
     * 加密dex
     *
     * @param targetDir
     */
    public static void encryptDex(String targetDir) {
        File file = new File(targetDir);
        if (file.exists()) {
            File[] dexFiles = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".dex");
                }
            });
            for (File dexFile : dexFiles) {
                try {
                    byte[] bytes = getBytes(dexFile);
                    byte[] encryptBytes = AES.encrypt(bytes);
                    File encryptfile = new File(dexFile.getParentFile().getPath() + "/secret-" + dexFile.getName());
                    if (!encryptfile.exists()) {
                        encryptfile.createNewFile();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(encryptfile);
                    fileOutputStream.write(encryptBytes);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 解密
     *
     * @param targetDir
     */
    public static void decryptDex(String targetDir) {
        File file = new File(targetDir);
        if (file.exists()) {
            File[] dexFiles = file.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".dex") && name.startsWith("secret-");
                }
            });
            for (File dexFile : dexFiles) {
                try {
                    byte[] bytes = getBytes(dexFile);
                    byte[] decryptBytes = AES.decrypt(bytes);
                    String fileName = dexFile.getName();
                    File decryptFile = new File(dexFile.getParentFile().getPath() + "/decrypt-" + fileName.substring(fileName.indexOf("-") + 1));
                    if (!decryptFile.exists()) {
                        decryptFile.createNewFile();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(decryptFile);
                    fileOutputStream.write(decryptBytes);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
