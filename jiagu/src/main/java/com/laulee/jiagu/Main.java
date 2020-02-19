package com.laulee.jiagu;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {

    private static String buildToolsPath = "/Users/laulee/Library/Android/sdk/build-tools/29.0.2";

    public static void main(String[] args) {

        System.out.println("开始加固");

        String targetDir = "app/build/outputs/apk/debug/app-debug";
        String apkPath = "app/build/outputs/apk/debug/app-debug.apk";
        //解压apk文件
        Zip.unZip(apkPath, targetDir);
        //获取apk解压文件中的dex文件
        File[] apkDexFiles = new File(targetDir).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".dex");
            }
        });

        System.out.println("apkDexFiles size is " + apkDexFiles.length);
        //创建一个壳dex，通过aar的方式解压得到classes.jar,通过dx命令将jar转化成dex
        String aarPath = "decryptdex/build/outputs/aar/decryptdex-release.aar";
        String aarUnZipPath = "app/build/outputs/apk/debug/decryptdex-release";
        //解压aar获取classes.jar
        Zip.unZip(aarPath, aarUnZipPath);
        File aarFile = new File(aarUnZipPath);
        if (aarFile.exists()) {
            File[] files = aarFile.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().equals("classes.jar");
                }
            });
            File classJarFile = files[0];
            //通过命令进行jar to dex 转化
            File classDexFile = jar2dex(classJarFile, aarUnZipPath);
            //采用主dex加密放入dump.dex中，其他dex加密
            File mainDexFile = null;
            byte[] mainDexByte = new byte[0];
            for (File apkDexFile : apkDexFiles) {
                //如果是主dex 保存下来
                if (apkDexFile.getName().endsWith("classes.dex")) {
                    try {
                        mainDexFile = apkDexFile;
                        AES.encrypt2File(apkDexFile);
//                        mainDexByte = AES.encrypt(Util.getBytes(apkDexFile));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                AES.encrypt(apkDexFile);
            }
            //将主dex加载到壳dex文件中
            if (mainDexByte != null) {
                //创建一个byte[] =壳dex长度+maindex长度+4字节的长度值
                try {
                    byte[] classesBytes = Util.getBytes(classDexFile);
//                    byte[] mergeBytes = new byte[classesBytes.length + mainDexByte.length + 4];

                    //将壳dex拷贝到合并dex
//                    System.arraycopy(classesBytes, 0, mergeBytes, 0, classesBytes.length);
                    //将main拷贝到合并dex
//                    System.arraycopy(mainDexByte, 0, mergeBytes, classesBytes.length, mainDexByte.length);

//                    int length = mainDexByte.length;
//                    byte[] mainLength = Util.toBytes(length);
//                    将maindex 长度写进去，这样可以反解密
//                    System.arraycopy(mainLength, 0, mergeBytes, mainDexByte.length + classesBytes.length, 4);

                    //将文件标记changeFileSize 文件实际长度
                    //changeSignature mergeDex
                    //changeCheckSum mergeDex

                    // 将壳classdex的写入到主classes.dex文件中
                    //将转化的classes.dex添加到apk解压文件中
                    FileOutputStream fileOutputStream = new FileOutputStream(mainDexFile);
                    fileOutputStream.write(classesBytes);
                    fileOutputStream.flush();
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //然后将合并后的dex，进行压缩打包apk
        String unsignApk = targetDir + "-unsign.apk";
        Zip.zip(targetDir, unsignApk);

        //打包完之后进行签名
        signApk(unsignApk, targetDir);

        //解密
//        AES.decrypt(targetDir);
    }

    /**
     * 将jar转换成dex
     *
     * @param classJarFile
     * @param aarUnZipPath
     * @return
     */
    private static File jar2dex(File classJarFile, String aarUnZipPath) {
        File classDexFile = new File(aarUnZipPath, "classes.dex");
        Process process = null;
        try {
            //dx 在Android/sdk/build-tools/android版本/文件夹下 通过环境变量去配置
            process = Runtime.getRuntime().exec(buildToolsPath + "/dx --dex --output " + classDexFile.getAbsolutePath() + " " + classJarFile.getAbsolutePath());
            process.waitFor();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (process.exitValue() != 0) {
            throw new RuntimeException("jar to dex fail");
        }
        return classDexFile;
    }

    /**
     * 添加签名
     *
     * @param unsignApk
     * @param targetDir
     */
    private static void signApk(String unsignApk, String targetDir) {
        File unsignFile = new File(unsignApk);
        File signApk = new File(targetDir + "-sign.apk");
        File alignedApk = new File(targetDir + "-align.apk");
        //对齐操作
        try {
            System.out.println(unsignFile.getAbsolutePath());
            Process process = Runtime.getRuntime().exec(
                    buildToolsPath + "/zipalign -f 4 "
                            + unsignFile.getAbsolutePath()
                            + " " + alignedApk.getAbsolutePath());
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new RuntimeException("zipalign error");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //签名
        File keyFile = new File("app/sign.jks");
        try {
            Process process = Runtime.getRuntime().exec(
                    buildToolsPath + "/apksigner sign --ks "
                            + keyFile.getAbsolutePath()
                            + " --ks-key-alias jiagu"
                            + " --ks-pass pass:123456"
                            + " --key-pass pass:123456"
                            + " --out " + signApk.getAbsolutePath()
                            + " " + alignedApk.getAbsolutePath());
            process.waitFor();
            if (process.exitValue() != 0) {
                throw new RuntimeException("sign error");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
