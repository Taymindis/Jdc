package com.github.taymindis.jdc;

//import com.google.gson.Gson;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static java.lang.reflect.Modifier.isPublic;

/**
 * Java Dynamic Function (zero downtime deployment)
 */
public class Jdc implements Serializable {


    private static final Map<String, Wired> backupCache = new HashMap<>();
    private static final Map<String, Wired> cache = new HashMap<>();
    
    public static Wired wireBean(Class<?> clazz) throws IOException, ClassNotFoundException, IllegalAccessException {
        return wireBean(clazz, getClassPath(clazz));
    }


    /**
     * @param clazz         the current class
     * @param classFilePath classFilePath /user/home/../../xxx.class
     * @param
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Wired wireBean(Class<?> clazz, String classFilePath) throws IOException, ClassNotFoundException, IllegalAccessException {
        if(!Serializable.class.isAssignableFrom(clazz)) {
            throw new NotSerializableException(clazz.getName().concat(" should be serializable"));
        }
        if(!isPublic(clazz.getModifiers())) {
            throw new IllegalAccessException(clazz.getName().concat(" should be public class"));
        }
        String key = generateKey(clazz);
        Wired wired;
        if (!has(key)) {
            if (classFilePath == null) {
                classFilePath = getClassPath(clazz);
            }
            Class<?> wiredClass = wireClass(clazz, classFilePath);
            try {
                Object wiredObject = instantiate(wiredClass);
                wired = new Wired(wiredObject, wiredObject.getClass(), key,
                        classFilePath);
                store(key, wired);
            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                wired = hasBackup(key) ? retrieveBackup(key) : null;
                e.printStackTrace();
            }
        } else {
            wired = retrieve(key);
        }
        return wired;
    }

    private static Object instantiate(Class<?> wiredClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return wiredClass.cast(wiredClass.getConstructor().newInstance());
    }

    protected static String getClassPath(Class<?> clz) {
        return clz.getProtectionDomain().getCodeSource().getLocation()
                .getPath().concat(clz.getCanonicalName().replace(".", File.separator ).concat(".class"));
    }


//    public static Object wireTrueBean(Class<?> clazz) throws IOException, ClassNotFoundException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
//        if(!Serializable.class.isAssignableFrom(clazz)) {
//            throw new NotSerializableException(clazz.getName().concat(" should be serializable"));
//        }
//        if(!isPublic(clazz.getModifiers())) {
//            throw new IllegalAccessException(clazz.getName().concat(" should be public class"));
//        }
//        Class<?> wiredClass = wireClass(clazz, getClassPath(clazz));
//        Object wiredObject = castObj(wiredClass.getConstructor().newInstance());
////        wired = new Wired(thatObject, thatObject.getClass(), key);
////        store(key, wired);
//        return wiredObject;
//    }

    private static Object castObj(Object o) throws IOException, ClassNotFoundException {
        if (o != null) {
            ByteArrayOutputStream baous = new ByteArrayOutputStream();
            {
                ObjectOutputStream oos = new ObjectOutputStream(baous);
                try {
                    oos.writeObject(o);
                } finally {
                    try {
                        oos.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            byte[] bb = baous.toByteArray();
            if (bb != null && bb.length > 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(bb);
                ObjectInputStream ois = new ObjectInputStream(bais);
                try {
                    Object res = ois.readObject();
                    return res;
                } finally {
                    ois.close();
                }
            }
        }
        throw new NullPointerException("Unable to instantiate the class");
    }

    /**
     * Although it's good but it's a dependencies required
     **/
//    private static <T>  T castObj2(Object o, Class clz) throws IOException, ClassNotFoundException {
//        Gson gson = new Gson();
//        T parsedObj = (T) gson.fromJson(gson.toJson(o), clz);
//        return parsedObj;
//    }


    private static Class<?> wireClass(Class<?> clazz, String classFilePath) {
        RuntimeClassLoader cl = new RuntimeClassLoader(clazz);
        try {
           return classFilePath != null ? cl.load(classFilePath) : cl.load();
//            if (!(instance instanceof Serializable)) {
//                throw new NotSerializableException("Please serialize your Class with implements Serializable");
//            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fall back to initial classloading instance if backup not found

        }
        return null;
    }


    public static boolean has(String key) {
        return cache.containsKey(key);
    }

    public static void store(String key, Wired wired) {
        cache.put(key, wired);
    }

    public static Wired retrieve(String key) {
        return cache.get(key);
    }

    public static boolean hasBackup(String key) {
        return backupCache.containsKey(key);
    }

    public static void storeBackup(String key, Wired wired) {
        backupCache.put(key, wired);
    }

    public static Wired retrieveBackup(String key) {
        return backupCache.get(key);
    }

    public static void reload() {
        reload(false);
    }

    public synchronized static void reload(boolean rollbackable) {
        if (rollbackable) {
            backupCache.clear();
            backupCache.putAll(cache);
        }

//        List<String> deletedKeys = new ArrayList<>();
        Class<?> wiredClass;
        for (Wired oldWired : cache.values()) {
            try {
                wiredClass = wireClass(oldWired.getClz(), oldWired.getClassPath());
                Object instance = instantiate(wiredClass);
                oldWired.setClz(instance.getClass());
                oldWired.setCtx(instance);
            } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

//        for(String key:deletedKeys) {
//            cache.remove(key);
//        }
    }

//    public static void reload(Class<?> clazz) {
//        reload(clazz, false);
//    }
//
//    public static void reload(Class<?> clazz, boolean rollbackable) {
//        String key = generateKey(clazz);
//        if (rollbackable && cache.containsKey(key)) {
//            storeBackup(key, retrieve(key));
//        }
//        cache.remove(key);
//    }

    // not thread  safe
    public static void delete(String key) {
        cache.remove(key);
    }


    // not thread  safe
    public static void rollback() {
        cache.clear();
        cache.putAll(backupCache);
    }

    public static void rollback(Class<?> clazz) {
        String key = generateKey(clazz);
        store(key, retrieveBackup(key));
    }

    protected static String generateKey(Class<?> clazz) {
        return clazz.getName();
    }

}
