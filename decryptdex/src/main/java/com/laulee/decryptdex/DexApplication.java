package com.laulee.decryptdex;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by laulee on 2020-02-18.
 */
public class DexApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        File dirFile = new File(getApplicationInfo().sourceDir);
        //保密模式创建文件
        File unzipFile = getDir("dex_apk", MODE_PRIVATE);
        File app = new File(unzipFile, "app");
        List<File> decryptDexs = new ArrayList<>();
        if (!app.exists()) {
            Zip.unZip(dirFile.getAbsolutePath(), app.getAbsolutePath());

            File[] files = app.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(".dex");
                }
            });
            Log.d("DexApplication", "dex size = " + files.length);
            //解密dex
            File shellDex = null;
            File mainDex = null;
            for (File file : files) {
                String fileName = file.getName();
                //壳dex不解密 保存下来
                if (fileName.equals("classes.dex")) {
                    shellDex = file;
                } else {
                    //将其他dex解密并重新写入
                    AES.decrypt(file);
                    //解密main
                    if (fileName.equals("dump.dex")) {
                        Log.d("DexApplication", "dump dex");
                        mainDex = file;
                    } else {
                        Log.d("DexApplication add", fileName);
                        decryptDexs.add(file);
                    }
                }
            }

            try {
                FileOutputStream fileOutputStream = new FileOutputStream(shellDex);
                fileOutputStream.write(AES.getBytes(mainDex));
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.d("DexApplication add", shellDex.getName());
            decryptDexs.add(shellDex);

            Log.d("DexApplication", "decryptDexs size = " + files.length);

            try {
                V19.install(getClassLoader(), decryptDexs, unzipFile);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }
}
