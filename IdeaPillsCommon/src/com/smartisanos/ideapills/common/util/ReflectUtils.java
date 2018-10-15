package com.smartisanos.ideapills.common.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;

public class ReflectUtils {

    public static Constructor findConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        Constructor constructor = null;
        try {
            constructor = clazz.getDeclaredConstructor(parameterTypes);
        } catch (Exception e) {
            handleReflectionException(e);
        }
        return constructor;
    }

    public static Class findClass(String className) {
        Class clazz = null;
        try {
            clazz = Class.forName(className);
        } catch (Exception e) {
            handleReflectionException(e);
        }
        return clazz;
    }

    public static Method findMethod(Class<?> clazz, String name) {
        return findMethod(clazz, name, new Class<?>[0]);
    }

    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            Class<?> searchType = clazz;
            while (searchType != null) {
                Method[] methods = (searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods());
                for (Method method : methods) {
                    if (name.equals(method.getName()) &&
                            (paramTypes == null || Arrays.equals(paramTypes, method.getParameterTypes()))) {
                        return method;
                    }
                }
                searchType = searchType.getSuperclass();
            }
        } catch (Exception ex) {
            handleReflectionException(ex);
        }
        return null;
    }

    public static Object invokeMethod(Method method, Object target) {
        return invokeMethod(method, target, new Object[0]);
    }

    public static Object invokeMethod(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception ex) {
            handleReflectionException(ex);
        }
        throw new IllegalStateException("Should never get here");
    }

    public static Object invokeNewInstance(Constructor constructor, Object... args) {
        try {
            return constructor.newInstance(args);
        } catch (Exception ex) {
            handleReflectionException(ex);
        }
        throw new IllegalStateException("Should never get here");
    }

    public static void handleReflectionException(Exception ex) {
        if (ex instanceof NoSuchMethodException) {
            throw new IllegalStateException("Method not found: " + ex.getMessage());
        }
        if (ex instanceof IllegalAccessException) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage());
        }
        if (ex instanceof InvocationTargetException) {
            throw new IllegalStateException("InvocationTargetException: " + ex.getMessage());
        }
        if (ex instanceof ClassNotFoundException) {
            throw new IllegalStateException("Class not found " + ex.getMessage());
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        throw new UndeclaredThrowableException(ex);
    }
}
