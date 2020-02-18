package com.laulee.decryptdex;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Installer for platform versions 19.
 */
public final class V19 {

    private static String TAG = "V19";

    public static void install(ClassLoader loader,
                               List<? extends File> additionalClassPathEntries,
                               File optimizedDirectory)
            throws IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
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
                suppressedExceptions));
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
        }
    }

    /**
     * A wrapper around
     * {@code private static final dalvik.system.DexPathList#makeDexElements}.
     */
    private static Object[] makeDexElements(
            Object dexPathList, ArrayList<File> files, File optimizedDirectory,
            ArrayList<IOException> suppressedExceptions)
            throws IllegalAccessException, InvocationTargetException,
            NoSuchMethodException {
        Method makeDexElements = Build.VERSION.SDK_INT >= 23 ? findMethod(dexPathList, "makeDexElements", List.class, File.class, List.class) :
                findMethod(dexPathList, "makeDexElements", ArrayList.class, File.class,
                        ArrayList.class);

        return (Object[]) makeDexElements.invoke(dexPathList, files, optimizedDirectory,
                suppressedExceptions);
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
}