/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.laulee.decryptdex;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import dalvik.system.DexFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipFile;

/**
 * MultiDex patches {@link Context#getClassLoader() the application context class
 * loader} in order to load classes from more than one dex file. The primary
 * {@code classes.dex} must contain the classes necessary for calling this
 * class methods. Secondary dex files named classes2.dex, classes3.dex... found
 * in the application apk will be added to the classloader after first call to
 * {@link #(Context)}.
 * <p>
 * <p/>
 * This library provides compatibility for platforms with API level 4 through 20. This library does
 * nothing on newer versions of the platform which provide built-in support for secondary dex files.
 */
public final class MultiDex {

    static final String TAG = "MultiDex";

    private static final String OLD_SECONDARY_FOLDER_NAME = "secondary-dexes";

    private static final String CODE_CACHE_NAME = "code_cache";

    private static final String CODE_CACHE_SECONDARY_FOLDER_NAME = "secondary-dexes";

    private static final int MAX_SUPPORTED_SDK_VERSION = 20;

    private static final int MIN_SDK_VERSION = 4;

    private static final int VM_WITH_MULTIDEX_VERSION_MAJOR = 2;

    private static final int VM_WITH_MULTIDEX_VERSION_MINOR = 1;

    private static final String NO_KEY_PREFIX = "";

    private static final Set<File> installedApk = new HashSet<File>();

    static final String DEX_SUFFIX = ".dex";

    static final String EXTRACTED_SUFFIX = ".zip";

    private static final boolean IS_VM_MULTIDEX_CAPABLE =
            isVMMultidexCapable(System.getProperty("java.vm.version"));

    private MultiDex() {
    }

    /**
     * Identifies if the current VM has a native support for multidex, meaning there is no need for
     * additional installation by this library.
     *
     * @return true if the VM handles multidex
     */
    /* package visible for test */
    static boolean isVMMultidexCapable(String versionString) {
        boolean isMultidexCapable = false;
        if (versionString != null) {
            Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?").matcher(versionString);
            if (matcher.matches()) {
                try {
                    int major = Integer.parseInt(matcher.group(1));
                    int minor = Integer.parseInt(matcher.group(2));
                    isMultidexCapable = (major > VM_WITH_MULTIDEX_VERSION_MAJOR)
                            || ((major == VM_WITH_MULTIDEX_VERSION_MAJOR)
                            && (minor >= VM_WITH_MULTIDEX_VERSION_MINOR));
                } catch (NumberFormatException e) {
                    // let isMultidexCapable be false
                }
            }
        }
        Log.i(TAG, "VM with version " + versionString +
                (isMultidexCapable ?
                        " has multidex support" :
                        " does not have multidex support"));
        return isMultidexCapable;
    }

    public static void installSecondaryDexes(ClassLoader loader, File dexDir,
                                             List<? extends File> files)
            throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException,
            InvocationTargetException, NoSuchMethodException, IOException, SecurityException,
            ClassNotFoundException, InstantiationException {
        if (!files.isEmpty()) {
            if (Build.VERSION.SDK_INT >= 19) {
                V19.install(loader, files, dexDir);
            } else if (Build.VERSION.SDK_INT >= 14) {
                V14.install(loader, files);
            } else {
                V4.install(loader, files);
            }
        }
    }

    /**
     * Locates a given field anywhere in the class inheritance hierarchy.
     *
     * @param instance an object to search the field into.
     * @param name     field name
     * @return a field object
     * @throws if the field cannot be located
     */
    private static Field findField(Object instance, String name) throws NoSuchFieldException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Field field = clazz.getDeclaredField(name);


                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }

                return field;
            } catch (NoSuchFieldException e) {
                // ignore and search next
            }
        }

        throw new NoSuchFieldException("Field " + name + " not found in " + instance.getClass());
    }

    /**
     * Locates a given method anywhere in the class inheritance hierarchy.
     *
     * @param instance       an object to search the method into.
     * @param name           method name
     * @param parameterTypes method parameter types
     * @return a method object
     * @throws if the method cannot be located
     */
    private static Method findMethod(Object instance, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        for (Class<?> clazz = instance.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Method method = clazz.getDeclaredMethod(name, parameterTypes);


                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }

                return method;
            } catch (NoSuchMethodException e) {
                // ignore and search next
            }
        }

        throw new NoSuchMethodException("Method " + name + " with parameters " +
                Arrays.asList(parameterTypes) + " not found in " + instance.getClass());
    }

    /**
     * Replace the value of a field containing a non null array, by a new array containing the
     * elements of the original array plus the elements of extraElements.
     *
     * @param instance      the instance whose field is to be modified.
     * @param fieldName     the field to modify.
     * @param extraElements elements to append at the end of the array.
     */
    private static void expandFieldArray(Object instance, String fieldName,
                                         Object[] extraElements) throws NoSuchFieldException, IllegalArgumentException,
            IllegalAccessException {
        Field jlrField = findField(instance, fieldName);
        Object[] original = (Object[]) jlrField.get(instance);
        Object[] combined = (Object[]) Array.newInstance(
                original.getClass().getComponentType(), original.length + extraElements.length);
        System.arraycopy(original, 0, combined, 0, original.length);
        System.arraycopy(extraElements, 0, combined, original.length, extraElements.length);
        jlrField.set(instance, combined);
    }

    private static void clearOldDexDir(Context context) throws Exception {
        File dexDir = new File(context.getFilesDir(), OLD_SECONDARY_FOLDER_NAME);
        if (dexDir.isDirectory()) {
            Log.i(TAG, "Clearing old secondary dex dir (" + dexDir.getPath() + ").");
            File[] files = dexDir.listFiles();
            if (files == null) {
                Log.w(TAG, "Failed to list secondary dex dir content (" + dexDir.getPath() + ").");
                return;
            }
            for (File oldFile : files) {
                Log.i(TAG, "Trying to delete old file " + oldFile.getPath() + " of size "
                        + oldFile.length());
                if (!oldFile.delete()) {
                    Log.w(TAG, "Failed to delete old file " + oldFile.getPath());
                } else {
                    Log.i(TAG, "Deleted old file " + oldFile.getPath());
                }
            }
            if (!dexDir.delete()) {
                Log.w(TAG, "Failed to delete secondary dex dir " + dexDir.getPath());
            } else {
                Log.i(TAG, "Deleted old secondary dex dir " + dexDir.getPath());
            }
        }
    }

    private static File getDexDir(Context context, File dataDir, String secondaryFolderName)
            throws IOException {
        File cache = new File(dataDir, CODE_CACHE_NAME);
        try {
            mkdirChecked(cache);
        } catch (IOException e) {
            /* If we can't emulate code_cache, then store to filesDir. This means abandoning useless
             * files on disk if the device ever updates to android 5+. But since this seems to
             * happen only on some devices running android 2, this should cause no pollution.
             */
            cache = new File(context.getFilesDir(), CODE_CACHE_NAME);
            mkdirChecked(cache);
        }
        File dexDir = new File(cache, secondaryFolderName);
        mkdirChecked(dexDir);
        return dexDir;
    }

    private static void mkdirChecked(File dir) throws IOException {
        dir.mkdir();
        if (!dir.isDirectory()) {
            File parent = dir.getParentFile();
            if (parent == null) {
                Log.e(TAG, "Failed to create dir " + dir.getPath() + ". Parent file is null.");
            } else {
                Log.e(TAG, "Failed to create dir " + dir.getPath() +
                        ". parent file is a dir " + parent.isDirectory() +
                        ", a file " + parent.isFile() +
                        ", exists " + parent.exists() +
                        ", readable " + parent.canRead() +
                        ", writable " + parent.canWrite());
            }
            throw new IOException("Failed to create directory " + dir.getPath());
        }
    }

    /**
     * Installer for platform versions 19.
     */
    private static final class V19 {

        static void install(ClassLoader loader,
                            List<? extends File> additionalClassPathEntries,
                            File optimizedDirectory)
                throws IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, InvocationTargetException, NoSuchMethodException,
                IOException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.BaseDexClassLoader. We modify its
             * dalvik.system.DexPathList pathList field to append additional DEX
             * file entries.
             */
            Field pathListField = findField(loader, "pathList");
            Object dexPathList = pathListField.get(loader);
            ArrayList<IOException> suppressedExceptions = new ArrayList<IOException>();
            expandFieldArray(dexPathList, "dexElements", makeDexElements(dexPathList,
                    new ArrayList<File>(additionalClassPathEntries), optimizedDirectory,
                    suppressedExceptions, loader));
            if (suppressedExceptions.size() > 0) {
                for (IOException e : suppressedExceptions) {
                    Log.w(TAG, "Exception in makeDexElement", e);
                }
                Field suppressedExceptionsField =
                        findField(dexPathList, "dexElementsSuppressedExceptions");
                IOException[] dexElementsSuppressedExceptions =
                        (IOException[]) suppressedExceptionsField.get(dexPathList);

                if (dexElementsSuppressedExceptions == null) {
                    dexElementsSuppressedExceptions =
                            suppressedExceptions.toArray(
                                    new IOException[suppressedExceptions.size()]);
                } else {
                    IOException[] combined =
                            new IOException[suppressedExceptions.size() +
                                    dexElementsSuppressedExceptions.length];
                    suppressedExceptions.toArray(combined);
                    System.arraycopy(dexElementsSuppressedExceptions, 0, combined,
                            suppressedExceptions.size(), dexElementsSuppressedExceptions.length);
                    dexElementsSuppressedExceptions = combined;
                }

                suppressedExceptionsField.set(dexPathList, dexElementsSuppressedExceptions);

                IOException exception = new IOException("I/O exception during makeDexElement");
                exception.initCause(suppressedExceptions.get(0));
                throw exception;
            }
        }

        /**
         * A wrapper around
         * {@code private static final dalvik.system.DexPathList#makeDexElements}.
         */
        private static Object[] makeDexElements(
                Object dexPathList, ArrayList<File> files, File optimizedDirectory,
                ArrayList<IOException> suppressedExceptions, ClassLoader classloader)
                throws IllegalAccessException, InvocationTargetException,
                NoSuchMethodException {

            //7.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Method makeDexElements =
                        findMethod(dexPathList, "makeDexElements", List.class, File.class,
                                List.class, ClassLoader.class);
                return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory,
                        suppressedExceptions, classloader);
            }
            //6.0+
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Method makeDexElements =
                        findMethod(dexPathList, "makePathElements", List.class, File.class,
                                List.class);
                return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory,
                        suppressedExceptions);
            }
            //4.4.4-5.0+
            else {
                Method makeDexElements =
                        findMethod(dexPathList, "makeDexElements", ArrayList.class, File.class,
                                ArrayList.class);

                return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory,
                        suppressedExceptions);
            }
        }
    }

    /**
     * Installer for platform versions 14, 15, 16, 17 and 18.
     */
    private static final class V14 {

        private interface ElementConstructor {
            Object newInstance(File file, DexFile dex)
                    throws IllegalArgumentException, InstantiationException,
                    IllegalAccessException, InvocationTargetException, IOException;
        }

        /**
         * Applies for ICS and early JB (initial release and MR1).
         */
        private static class ICSElementConstructor implements ElementConstructor {
            private final Constructor<?> elementConstructor;

            ICSElementConstructor(Class<?> elementClass)
                    throws SecurityException, NoSuchMethodException {
                elementConstructor =
                        elementClass.getConstructor(File.class, ZipFile.class, DexFile.class);
                elementConstructor.setAccessible(true);
            }

            @Override
            public Object newInstance(File file, DexFile dex)
                    throws IllegalArgumentException, InstantiationException,
                    IllegalAccessException, InvocationTargetException, IOException {
                return elementConstructor.newInstance(file, new ZipFile(file), dex);
            }
        }

        /**
         * Applies for some intermediate JB (MR1.1).
         * <p>
         * See Change-Id: I1a5b5d03572601707e1fb1fd4424c1ae2fd2217d
         */
        private static class JBMR11ElementConstructor implements ElementConstructor {
            private final Constructor<?> elementConstructor;

            JBMR11ElementConstructor(Class<?> elementClass)
                    throws SecurityException, NoSuchMethodException {
                elementConstructor = elementClass
                        .getConstructor(File.class, File.class, DexFile.class);
                elementConstructor.setAccessible(true);
            }

            @Override
            public Object newInstance(File file, DexFile dex)
                    throws IllegalArgumentException, InstantiationException,
                    IllegalAccessException, InvocationTargetException {
                return elementConstructor.newInstance(file, file, dex);
            }
        }

        /**
         * Applies for latest JB (MR2).
         * <p>
         * See Change-Id: Iec4dca2244db9c9c793ac157e258fd61557a7a5d
         */
        private static class JBMR2ElementConstructor implements ElementConstructor {
            private final Constructor<?> elementConstructor;

            JBMR2ElementConstructor(Class<?> elementClass)
                    throws SecurityException, NoSuchMethodException {
                elementConstructor = elementClass
                        .getConstructor(File.class, Boolean.TYPE, File.class, DexFile.class);
                elementConstructor.setAccessible(true);
            }

            @Override
            public Object newInstance(File file, DexFile dex)
                    throws IllegalArgumentException, InstantiationException,
                    IllegalAccessException, InvocationTargetException {
                return elementConstructor.newInstance(file, Boolean.FALSE, file, dex);
            }
        }

        private static final int EXTRACTED_SUFFIX_LENGTH =
                EXTRACTED_SUFFIX.length();

        private final ElementConstructor elementConstructor;

        static void install(ClassLoader loader,
                            List<? extends File> additionalClassPathEntries)
                throws IOException, SecurityException, IllegalArgumentException,
                ClassNotFoundException, NoSuchMethodException, InstantiationException,
                IllegalAccessException, InvocationTargetException, NoSuchFieldException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.BaseDexClassLoader. We modify its
             * dalvik.system.DexPathList pathList field to append additional DEX
             * file entries.
             */
            Field pathListField = findField(loader, "pathList");
            Object dexPathList = pathListField.get(loader);
            Object[] elements = new V14().makeDexElements(additionalClassPathEntries);
            try {
                expandFieldArray(dexPathList, "dexElements", elements);
            } catch (NoSuchFieldException e) {
                // dexElements was renamed pathElements for a short period during JB development,
                // eventually it was renamed back shortly after.
                Log.w(TAG, "Failed find field 'dexElements' attempting 'pathElements'", e);
                expandFieldArray(dexPathList, "pathElements", elements);
            }
        }

        private V14() throws ClassNotFoundException, SecurityException, NoSuchMethodException {
            ElementConstructor constructor;
            Class<?> elementClass = Class.forName("dalvik.system.DexPathList$Element");
            try {
                constructor = new ICSElementConstructor(elementClass);
            } catch (NoSuchMethodException e1) {
                try {
                    constructor = new JBMR11ElementConstructor(elementClass);
                } catch (NoSuchMethodException e2) {
                    constructor = new JBMR2ElementConstructor(elementClass);
                }
            }
            this.elementConstructor = constructor;
        }

        /**
         * An emulation of {@code private static final dalvik.system.DexPathList#makeDexElements}
         * accepting only extracted secondary dex files.
         * OS version is catching IOException and just logging some of them, this version is letting
         * them through.
         */
        private Object[] makeDexElements(List<? extends File> files)
                throws IOException, SecurityException, IllegalArgumentException,
                InstantiationException, IllegalAccessException, InvocationTargetException {
            Object[] elements = new Object[files.size()];
            for (int i = 0; i < elements.length; i++) {
                File file = files.get(i);
                elements[i] = elementConstructor.newInstance(
                        file,
                        DexFile.loadDex(file.getPath(), optimizedPathFor(file), 0));
            }
            return elements;
        }

        /**
         * Converts a zip file path of an extracted secondary dex to an output file path for an
         * associated optimized dex file.
         */
        private static String optimizedPathFor(File path) {
            // Any reproducible name ending with ".dex" should do but lets keep the same name
            // as DexPathList.optimizedPathFor

            File optimizedDirectory = path.getParentFile();
            String fileName = path.getName();
            String optimizedFileName =
                    fileName.substring(0, fileName.length() - EXTRACTED_SUFFIX_LENGTH)
                            + DEX_SUFFIX;
            File result = new File(optimizedDirectory, optimizedFileName);
            return result.getPath();
        }
    }

    /**
     * Installer for platform versions 4 to 13.
     */
    private static final class V4 {
        static void install(ClassLoader loader,
                            List<? extends File> additionalClassPathEntries)
                throws IllegalArgumentException, IllegalAccessException,
                NoSuchFieldException, IOException {
            /* The patched class loader is expected to be a descendant of
             * dalvik.system.DexClassLoader. We modify its
             * fields mPaths, mFiles, mZips and mDexs to append additional DEX
             * file entries.
             */
            int extraSize = additionalClassPathEntries.size();

            Field pathField = findField(loader, "path");

            StringBuilder path = new StringBuilder((String) pathField.get(loader));
            String[] extraPaths = new String[extraSize];
            File[] extraFiles = new File[extraSize];
            ZipFile[] extraZips = new ZipFile[extraSize];
            DexFile[] extraDexs = new DexFile[extraSize];
            for (ListIterator<? extends File> iterator = additionalClassPathEntries.listIterator();
                 iterator.hasNext(); ) {
                File additionalEntry = iterator.next();
                String entryPath = additionalEntry.getAbsolutePath();
                path.append(':').append(entryPath);
                int index = iterator.previousIndex();
                extraPaths[index] = entryPath;
                extraFiles[index] = additionalEntry;
                extraZips[index] = new ZipFile(additionalEntry);
                extraDexs[index] = DexFile.loadDex(entryPath, entryPath + ".dex", 0);
            }

            pathField.set(loader, path.toString());
            expandFieldArray(loader, "mPaths", extraPaths);
            expandFieldArray(loader, "mFiles", extraFiles);
            expandFieldArray(loader, "mZips", extraZips);
            expandFieldArray(loader, "mDexs", extraDexs);
        }
    }

}
