package com.laulee.jiagu;

import com.laulee.jiagu.xmleditor.chunk.AttributeData;
import com.laulee.jiagu.xmleditor.chunk.StartTagChunk;
import com.laulee.jiagu.xmleditor.main.ParserChunkUtils;
import com.laulee.jiagu.xmleditor.main.Test;
import com.laulee.jiagu.xmleditor.main.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Main {

    private static String buildToolsPath = "/Users/laulee/Library/Android/sdk/build-tools/29.0.2";

    public static void main(String[] args) {

        System.out.println("开始加固");

        String targetDir = "app/build/outputs/apk/debug/app-debug";
        String apkPath = "app/build/outputs/apk/debug/app-debug.apk";
        //解压apk文件
        System.out.println("解压apk文件");
        Zip.unZip(apkPath, targetDir);
        //获取apk解压文件中的dex文件
        File[] apkDexFiles = new File(targetDir).listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().endsWith(".dex");
            }
        });

        //创建一个壳dex，通过aar的方式解压得到classes.jar,通过dx命令将jar转化成dex
        System.out.println("创建一个壳dex，通过aar的方式解压得到classes.jar,通过dx命令将jar转化成dex");
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
            System.out.println("将主dex加载到壳dex文件中");
            if (mainDexByte != null) {
                //创建一个byte[] =壳dex长度+maindex长度+4字节的长度值
                try {
                    byte[] classesBytes = AES.getBytes(classDexFile);
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


        String manifestPath = targetDir + "/AndroidManifest.xml";

        //获取applicationName并保存
        saveApplocation(targetDir, manifestPath);

        //修改manifest文件中的application的name,这里使用AXMLEditor
        changeMainfest(targetDir, manifestPath);

        //然后将合并后的dex，进行压缩打包apk
        System.out.println("然后将合并后的dex,进行压缩打包apk");
        String unsignApk = targetDir + "-unsign.apk";
        Zip.zip(targetDir, unsignApk);

        //打包完之后进行签名
        signApk(unsignApk, targetDir);
        System.out.println("签名完成,加固完毕");
    }

    /**
     * 修改
     *
     * @param manifestPath
     */
    private static void changeMainfest(String targetDir, String manifestPath) {

        System.out.println("修改manifest文件中的application");
        //修改属性
        String outputMainfest = targetDir + "/AndroidManifest_out.xml";
        String[] args = new String[8];
        args[0] = "-attr";
        args[1] = "-m";
        args[2] = "application";
        args[3] = "package";
        args[4] = "name";
        args[5] = "com.laulee.decryptdex.DexApplication";
        args[6] = manifestPath;
        args[7] = outputMainfest;

        //这里有个问题，必须application里面配置name!!!必须放在主题和icon的后面，但是不能放到最后!!!
        Test.run(manifestPath, outputMainfest, args);

        //替换原来的manifest.xml
        System.out.println("替换原来的manifest.xml");
        File file = new File(outputMainfest);
        if (file.exists()) {
            File oldMainfest = new File(manifestPath);
            oldMainfest.delete();
            file.renameTo(new File(manifestPath));
        }
    }

    /**
     * 获取application并保存
     *
     * @param manifestPath
     */
    private static void saveApplocation(String targetDir, String manifestPath) {
        String appNameApplication = getAppNameApplication(new File(manifestPath));
        //如果不是空
        if (appNameApplication != null && appNameApplication.length() > 0) {
            //写入到app.txt文件中
            File file = new File(targetDir + "/app.txt");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                byte[] bytes = appNameApplication.getBytes();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                fileOutputStream.write(bytes);
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 获取原manifest--app:name
     *
     * @param manifestPath
     * @return
     */
    private static String getAppNameApplication(File manifestPath) {
        ByteArrayOutputStream byteArrayOutputStream;
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(manifestPath);
            byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            boolean var8 = false;
            int len;
            while ((len = fileInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            ParserChunkUtils.xmlStruct.byteSrc = byteArrayOutputStream.toByteArray();
        } catch (Exception var33) {
            System.out.println("parse xml error:" + var33.toString());
        } finally {
            try {
                fileInputStream.close();
                fileInputStream.close();
            } catch (Exception e) {
            }
        }

        String tag = "application";
        String attrName = "name";
        ParserChunkUtils.parserXml();
        String appName = "";
        for (StartTagChunk chunk : ParserChunkUtils.xmlStruct.startTagChunkList) {
            int tagNameIndex = Utils.byte2int(chunk.name);
            String tagNameTmp = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(tagNameIndex);

            if (tag.equals(tagNameTmp)) {
                //过滤标签
                if (tag.equals("application") || tag.equals("manifest")) {
                    for (AttributeData data : chunk.attrList) {
                        String attrNameTemp1 = ParserChunkUtils.xmlStruct.stringChunk.stringContentList.get(data.name);
                        if (attrName.equals(attrNameTemp1)) {
                            if (chunk.attrList.size() == 1) {
                                return appName;
                            }
                            appName = data.getData();
                            System.out.println("appName ==>" + appName);
                            return appName;
                        }
                    }
                }
            }
        }
        return appName;
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
