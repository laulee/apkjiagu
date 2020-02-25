package com.laulee.decryptdex;

import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by laulee on 2020-02-18.
 */
public class DexApplication extends Application {

    String realApplication;

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
                        mainDex = file;
                    } else {
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
                //7.0+反射makeDexElement参数多ClassLoader
                MultiDex.installSecondaryDexes(getClassLoader(), unzipFile, decryptDexs);
                Log.d("DexApplication", "add dex success");
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //替换自己的application
        File appTxt = new File(app.getAbsolutePath() + "/app.txt");
        if (appTxt.exists()) {
            try {
                byte[] bytes = AES.getBytes(appTxt);
                realApplication = new String(bytes);
                System.out.println("读取appName" + realApplication);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!TextUtils.isEmpty(realApplication)) {
            bindRealApplication();
        }
    }

    private Application delegateApplication;

    /**
     * 替换真实的application
     */
    private void bindRealApplication() {
        try {
            Context mBaseContext = getBaseContext();
            //创建真实的application对象
            Class<?> delegateClass = Class.forName(realApplication);
            delegateApplication = (Application) delegateClass.newInstance();
            //获取application里面的attach方法，将mBase赋值给delegateApplication
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            attach.setAccessible(true);
            attach.invoke(delegateApplication, mBaseContext);

            //baseContext.mOuterContext
            //baseContext.mPackageInfo.mApplication
            //baseContext.mPackageInfo.mActivityThread.mInitialApplication
            //baseContext.mPackageInfo.mActivityThread.mAllApplications

            //替换系统中的application
            Class<?> contextImplClass = Class.forName("android.app.ContextImpl");
            Field mOuterContextField = contextImplClass.getDeclaredField("mOuterContext");
            mOuterContextField.setAccessible(true);
            mOuterContextField.set(mBaseContext, delegateApplication);

            //activityThread
            Field mainThreadField = contextImplClass.getDeclaredField("mMainThread");
            mainThreadField.setAccessible(true);
            Object mMainThread = mainThreadField.get(mBaseContext);

            //替换mInitialApplication
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Field mInitialApplicationField = activityThreadClass.getDeclaredField("mInitialApplication");
            mInitialApplicationField.setAccessible(true);
            mInitialApplicationField.set(mMainThread, delegateApplication);

            //mAllApplications
            Field mAllApplicationField = activityThreadClass.getDeclaredField("mAllApplications");
            mAllApplicationField.setAccessible(true);
            ArrayList<Application> applications = (ArrayList<Application>) mAllApplicationField.get(mMainThread);
            applications.remove(this);
            applications.add(delegateApplication);
            mAllApplicationField.set(mMainThread, applications);

            //LoadedApk
            Field mPackageInfoField = contextImplClass.getDeclaredField("mPackageInfo");
            mPackageInfoField.setAccessible(true);
            Object mPackageInfo = mPackageInfoField.get(mBaseContext);

            Class<?> loadedApk = Class.forName("android.app.LoadedApk");
            Field mApplicationField = loadedApk.getDeclaredField("mApplication");
            mApplicationField.setAccessible(true);
            mApplicationField.set(mPackageInfo, delegateApplication);

            //修改applicationinfo className LoadedApk
            Field mApplicationInfoField = loadedApk.getDeclaredField("mApplicationInfo");
            mApplicationInfoField.setAccessible(true);
            ApplicationInfo applicationInfo = (ApplicationInfo) mApplicationInfoField.get(mPackageInfo);
            applicationInfo.className = realApplication;

            delegateApplication.onCreate();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getPackageName() {
        //ProxyApplication对象的getPackageName()函数与ContentProvider对应的包名相同，
        // 就会复用ProxyApplication对象作为Context，而不会再创建一个新的packageContext。于是解决方案也很简单了
        if (!TextUtils.isEmpty(realApplication)) {
            return "";
        }
        return super.getPackageName();
    }
}
