package com.laulee.jiagu;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by laulee
 * on 2020-02-17.
 */
public class Zip {

    /**
     * 解压文件
     *
     * @param path
     * @param targetDir
     */
    public static void unZip(String path, String targetDir) {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(path);

            //创建解压文件夹
            File file = new File(targetDir);
            if (!file.exists()) {
                file.mkdirs();
            }
//            targetDir = targetDir + "/" + zipFile.getName();
            Enumeration<?> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) entries.nextElement();
                if (zipEntry.isDirectory()) {
                    //创建文件夹
                    String dirPath = targetDir + "/" + zipEntry.getName();
                    createDirIfNotExist(dirPath);
                } else {
                    File targetFile = new File(targetDir + "/" + zipEntry.getName());
                    createFileIfNotExist(targetFile);
                    InputStream inputStream = null;
                    FileOutputStream fileOutputStream = null;
                    try {
                        inputStream = zipFile.getInputStream(zipEntry);
                        fileOutputStream = new FileOutputStream(targetFile);
                        int length;
                        byte[] bytes = new byte[1024];
                        while ((length = inputStream.read(bytes)) != -1) {
                            fileOutputStream.write(bytes, 0, length);
                        }
                    } catch (Exception e) {

                    } finally {
                        try {
                            fileOutputStream.close();
                            inputStream.close();
                        } catch (Exception e) {

                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private static void createFileIfNotExist(File targetFile) throws IOException {
        createParentDirIfNotExist(targetFile);
        targetFile.createNewFile();
    }

    private static void createParentDirIfNotExist(File targetFile) {
        createDirIfNotExist(targetFile.getParentFile().getPath());
    }

    /**
     * 创建文件夹
     *
     * @param dirPath
     */
    private static void createDirIfNotExist(String dirPath) {
        File file = new File(dirPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    /**
     * 压缩
     *
     * @param zipFilePath
     * @param unsignApk
     */
    public static void zip(String zipFilePath, String unsignApk) {

        File zipFile = new File(zipFilePath);
        if (zipFile.exists()) {
            File unsignFile = new File(unsignApk);
            try {
                //创建文件
                CheckedOutputStream checkedOutputStream = new CheckedOutputStream(new FileOutputStream(unsignFile), new CRC32());
                ZipOutputStream zipOutputStream = new ZipOutputStream(checkedOutputStream);
                compress(zipFile, zipOutputStream, "");
                zipOutputStream.flush();
                zipOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 压缩
     *
     * @param zipFile
     * @param zipOutputStream
     * @param basePath
     */
    private static void compress(File zipFile, ZipOutputStream zipOutputStream, String basePath)
            throws Exception {
        if (zipFile.isDirectory()) {
            File[] files = zipFile.listFiles();
            for (File file : files) {
                compress(file, zipOutputStream, basePath + zipFile.getName() + "/");
            }
        } else {
            compressFile(zipFile, zipOutputStream, basePath);
        }

    }

    /**
     * @param zipFile
     * @param zipOutputStream
     * @param basePath
     */
    private static void compressFile(File zipFile, ZipOutputStream zipOutputStream, String basePath)
            throws Exception {
        String fullName = basePath + zipFile.getName();
        String[] fileNames = fullName.split("/");
        StringBuilder stringBuilder = new StringBuilder();
        if (fileNames.length > 1) {
            for (int i = 1; i < fileNames.length; i++) {
                stringBuilder.append("/");
                stringBuilder.append(fileNames[i]);
            }
        } else {
            stringBuilder.append("/");
        }

        ZipEntry zipEntry = new ZipEntry(stringBuilder.substring(1));
        zipOutputStream.putNextEntry(zipEntry);
        FileInputStream fileInputStream = new FileInputStream(zipFile);
        int length;
        byte[] data = new byte[2048];
        while ((length = fileInputStream.read(data, 0, 2048)) != -1) {
            zipOutputStream.write(data, 0, length);
        }
        fileInputStream.close();
        zipOutputStream.closeEntry();
    }
}
