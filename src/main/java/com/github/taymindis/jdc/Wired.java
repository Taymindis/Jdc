package com.github.taymindis.jdc;

import java.io.IOException;
import java.util.*;

public final class Wired<T> extends Object {

    private static final Map<Integer, Wired<?>> backupCache = new HashMap<>();
    private static final Map<Integer, Wired<?>> cache = new HashMap<>();

    private T ctx;
    private Class<T> clz;
    private Integer key;

    public Wired(T ctx, Class<T> clz, Integer key) {
        this.ctx = ctx;
        this.clz = clz;
        this.key = key;
    }

    protected Class<T> getClz() {
        return clz;
    }

    protected Integer getKey() {
        return key;
    }


    private void setCtx(Object ctx) {
        this.ctx = (T) ctx;
    }

    private void setClz(Class<?> clz) {
        this.clz = (Class<T>) clz;
    }

    private void setKey(Integer key) {
        this.key = key;
    }

    public T get(){
//        if(!cache.containsKey(key)) {
//            Jdc.useBean(clz);
//        }

        if (ctx == null) {
            throw new NoSuchElementException(ctx.getClass().getSimpleName().concat(" has not wired yet"));
        }
        return ctx;
    }

    public static boolean has(Integer key) {
        return cache.containsKey(key);
    }
    public static void store(Integer key, Wired wired) {
        cache.put(key, wired);
    }

    public static <T> Wired<T> retrieve(Integer key) {
       return (Wired<T>) cache.get(key);
    }

    public static boolean hasBackup(Integer key) {
        return backupCache.containsKey(key);
    }

    public static void storeBackup(Integer key, Wired wired) {
        backupCache.put(key, wired);
    }

    public static <T> Wired<T> retrieveBackup(Integer key) {
       return (Wired<T>) backupCache.get(key);
    }


    public static void reload() {
        reload(false);
    }

    public static void reload(boolean rollbackable) {
        if (rollbackable) {
            backupCache.clear();
            backupCache.putAll(cache);
        }

        Wired<?> oldWired, newWired;
        Integer oldKey, newKey;
        for (Map.Entry<Integer, Wired<?>> wiredEntry : cache.entrySet()) {
            oldWired = wiredEntry.getValue();
            oldKey = wiredEntry.getKey();
            try {
                newWired = Jdc.wireBean(oldWired.getClz(), null, true);
                newKey = newWired.getKey();
                if(!newKey.equals(oldKey)) {
                    // Safe delete
                    Wired.delete(oldKey);
                }
                oldWired.setClz(newWired.getClz());
                oldWired.setCtx(newWired.ctx);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

//    public static void reload(Class<?> clazz) {
//        reload(clazz, false);
//    }
//
//    public static void reload(Class<?> clazz, boolean rollbackable) {
//        Integer key = generateKey(clazz);
//        if (rollbackable && cache.containsKey(key)) {
//            storeBackup(key, retrieve(key));
//        }
//        cache.remove(key);
//    }

    // not thread  safe
    public static void delete(Integer key){
        cache.remove(key);
    }


    // not thread  safe
    public static void rollback() {
        cache.clear();
        cache.putAll(backupCache);
    }

    public static void rollback(Class<?> clazz) {
        Integer key = generateKey(clazz);
        store(key, retrieveBackup(key));
    }

    protected static Integer generateKey(Class<?> clazz) {
        return Objects.hash(clazz, clazz.getSimpleName());
    }
}
