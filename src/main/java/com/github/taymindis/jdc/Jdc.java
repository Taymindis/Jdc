package com.github.taymindis.jdc;

import java.io.*;
import java.lang.reflect.InvocationTargetException;

import static java.lang.reflect.Modifier.isPublic;

/**
 * Java Dynamic Function (zero downtime deployment)
 */
public class Jdc implements Serializable {

    public static <T> Wired<T> wireBean(Class<?> clazz) throws IOException, ClassNotFoundException, IllegalAccessException {
        return wireBean(clazz, null);
    }


    public static <T> Wired<T> wireBean(Class<?> clazz, String classFilePath) throws IOException, ClassNotFoundException, IllegalAccessException {
        return wireBean(clazz, classFilePath, false);
    }

    /**
     * @param clazz         the current class
     * @param classFilePath classFilePath /user/home/../../xxx.class
     * @param <T>
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static <T> Wired<T> wireBean(Class<?> clazz, String classFilePath, Boolean forceReload) throws IOException, ClassNotFoundException, IllegalAccessException {
        if(!Serializable.class.isAssignableFrom(clazz)) {
            throw new NotSerializableException(clazz.getName().concat(" should be serializable"));
        }
        if(!isPublic(clazz.getModifiers())) {
            throw new IllegalAccessException(clazz.getName().concat(" should be public class"));
        }
        Integer key = Wired.generateKey(clazz);
        Wired<T> wired;
        if (!Wired.has(key) || forceReload) {
            Object instance = wireClass(key, clazz, classFilePath);
            T thatObject = castObj(instance);
            wired = new Wired(thatObject, thatObject.getClass(), key);
            Wired.store(key, wired);
        } else {
            wired = Wired.retrieve(key);
        }
        return wired;
    }

    public static <T> T wireTrueBean(Class<?> clazz) throws IOException, ClassNotFoundException, IllegalAccessException {
        if(!Serializable.class.isAssignableFrom(clazz)) {
            throw new NotSerializableException(clazz.getName().concat(" should be serializable"));
        }
        if(!isPublic(clazz.getModifiers())) {
            throw new IllegalAccessException(clazz.getName().concat(" should be public class"));
        }
        Integer key = Wired.generateKey(clazz);
        Wired<T> wired;
        Object instance = wireClass(key, clazz, null);
        T thatObject = castObj(instance);
//        wired = new Wired(thatObject, thatObject.getClass(), key);
//        Wired.store(key, wired);
        return thatObject;
    }

    private static <T> T castObj(Object o) throws IOException, ClassNotFoundException {
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
                    T res = (T) ois.readObject();
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
//    private static <T> T castObj2(Object o, Class<T> clz) throws IOException, ClassNotFoundException {
//        Gson gson = new Gson();
//        T parsedObj = gson.fromJson(gson.toJson(o), clz);
//        return parsedObj;
//    }


    private static Object wireClass(Integer key, Class<?> clazz, String classFilePath) {
        RuntimeClassLoader cl = new RuntimeClassLoader(clazz);
        Class<?> thisClass;
        Object instance;
        try {
            thisClass = classFilePath != null ? cl.load("file:".concat(classFilePath)) : cl.load();
            instance = thisClass.newInstance();
//            if (!(instance instanceof Serializable)) {
//                throw new NotSerializableException("Please serialize your Class with implements Serializable");
//            }
        } catch (Exception e) {
            e.printStackTrace();
            // Fall back to initial classloading instance if backup not found
            try {
                instance = Wired.hasBackup(key) ? Wired.retrieveBackup(key).get() : clazz.getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException instantiationException) {
                instantiationException.printStackTrace();
                return null;
            }
        }
        return instance;
    }

}
