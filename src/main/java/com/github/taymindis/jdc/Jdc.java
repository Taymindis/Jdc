package com.github.taymindis.jdc;

//import com.google.gson.Gson;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

import static com.github.taymindis.jdc.WiredConstant.WIRE_PROXY_PREFIX;
import static com.github.taymindis.jdc.WiredConstant.WIRE_PROXY_SUFFIX;
import static java.lang.reflect.Modifier.isPublic;

/**
 * Java Dynamic Function (zero downtime deployment)
 */
public class Jdc implements Serializable {

    private static WiredClassLoader wiredClassLoader = null;
    private static boolean hotReloading = false;

    /**
     * @param clazz the current class
     * @param <T>   The return generic type
     * @return WiredContext
     * @throws IOException            IO Class not found
     * @throws IllegalAccessException Illegal Bean Access
     */
    public static <T> T wireBean(Class<?> clazz) throws IOException, IllegalAccessException {
        if (!Serializable.class.isAssignableFrom(clazz)) {
            throw new NotSerializableException(clazz.getName().concat(" should be serializable"));
        }
        if (!isPublic(clazz.getModifiers())) {
            throw new IllegalAccessException(clazz.getName().concat(" should be public class"));
        }
        String key = generateKey(clazz);
        Object wiredProxy;
        WiredClass wiredClass = wireClass(key, clazz);
        wiredProxy = getNewContext(wiredClass, clazz);
        return (T) wiredProxy;
    }

    /**
     * @param clazz the current class
     * @return WiredContext
     * @throws IOException            IO Class not found
     * @throws IllegalAccessException Illegal Bean Access
     */
    public static WiredMethod wireMethod(Class<?> clazz) throws IOException, IllegalAccessException {
        if (!Serializable.class.isAssignableFrom(clazz)) {
            throw new NotSerializableException(clazz.getName().concat(" should be serializable"));
        }
        if (!isPublic(clazz.getModifiers())) {
            throw new IllegalAccessException(clazz.getName().concat(" should be public class"));
        }
        String key = generateKey(clazz);
        WiredClass wiredClass = wireClass(key, clazz);
        WiredMethod wiredMethod = getMethodContext(wiredClass, clazz);
        return wiredMethod;
    }

    private static WiredMethod getMethodContext(WiredClass wiredClass, Class<?> originClass) {
        try {
            Class<?> newLoadedClass = wiredClass.getClazz();
            Object wiredObject = instantiate(newLoadedClass);

            WiredMethod wiredMethod = new WiredMethod();
            wiredMethod.setCtx(wiredObject);
            wiredMethod.setClassUsing(newLoadedClass);

            return wiredMethod;
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Object getNewContext(WiredClass wiredClass, Class<?> originClass) {
        try {
//            Class<?> wireProxyClass = wiredClass.getProxyClass();
//            if (wireProxyClass == null) {
            Class<?> wireProxyClass = Class.forName(originClass.getPackage().getName().concat(".").concat(WIRE_PROXY_PREFIX)
                    .concat(originClass.getSimpleName()).concat(WIRE_PROXY_SUFFIX));
            wiredClass.setProxyClass(wireProxyClass);
//            }

            Object wiredProxy = wireProxyClass.getConstructor().newInstance();

            Class<?> newLoadedClass = wiredClass.getClazz();
            Object wiredObject = instantiate(newLoadedClass);
            wireProxyClass.getDeclaredMethod("setCtx", Object.class).invoke(wiredProxy, wiredObject);
            wireProxyClass.getDeclaredMethod("set_wiringjdc_", boolean.class).invoke(wiredProxy, true);
            wireProxyClass.getDeclaredMethod("setClassUsing", Class.class).invoke(wiredProxy, newLoadedClass);

            return wiredProxy;
//                wiredContext = new WiredContext(wiredObject, wiredObject.getClass(), key,
//                        classFilePath);
//                store(key, wiredClass);
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
//                wiredContext = hasBackup(key) ? retrieveBackup(key) : null;
            e.printStackTrace();
        }
        return null;
    }

    private static Object instantiate(Class<?> wiredClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return wiredClass.cast(wiredClass.getConstructor().newInstance());
//        return castObj(wiredClass.getConstructor().newInstance());
    }

    protected static String getClassPath(Class<?> clz) {
        return clz.getProtectionDomain().getCodeSource().getLocation()
                .getPath().concat(clz.getName().replace(".", File.separator).concat(".class"));
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
    private static WiredClass wireClass(String key, Class<?> clazz) {
        if (hotReloading || wiredClassLoader == null) {
            wiredClassLoader = new WiredClassLoader();
            hotReloading = false;
        }
        try {
            Class<?> clz = wiredClassLoader.load(clazz);
            WiredClass wiredClass = new WiredClass(key, clz);
            return wiredClass;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Class<?> wireClass(Class<?> clazz) throws IOException, ClassNotFoundException {
        WiredClass wiredClass = wireClass(generateKey(clazz), clazz);
        return wiredClass.getClazz();
    }

//    public static void reload() {
//        reload(false);
//    }

//    public synchronized static void reload(boolean rollbackable) {
//        if (rollbackable) {
//            backupCache.clear();
//            backupCache.putAll(cache);
//        }
//
////        List<String> deletedKeys = new ArrayList<>();
//        WiredClass wiredClass;
//        for (WiredClass oldWiredClass : cache.values()) {
//            try {
//                wiredClass = wireClass(oldWiredClass.getKey(), oldWiredClass.getClazz(), oldWiredClass.getClassPath());
//                Object instance = instantiate(wiredClass);
//                oldWiredClass.setWiredClass(instance.getClass());
//                oldWiredClass.setCtx(instance);
//            } catch (IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException | IOException | ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//        }
//
////        for(String key:deletedKeys) {
////            cache.remove(key);
////        }
//    }

    public static void hotReload() {
        hotReloading = true;
    }

    protected static String generateKey(Class<?> clazz) {
        return clazz.getName();
    }

}
