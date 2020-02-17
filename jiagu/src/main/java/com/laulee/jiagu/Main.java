package com.laulee.jiagu;

public class Main {

    public static void main(String[] args) {
        System.out.println("Hello World!");

        String targetDir = "app/build/outputs/apk/debug/app-debug";
        String apkPath = "app/build/outputs/apk/debug/app-debug.apk";
        //解压apk文件
        Zip.unZip(apkPath, targetDir);

        //过滤dex文件加密
        AES.encrypt(targetDir);

        //解密
        AES.decrypt(targetDir);
    }
}
