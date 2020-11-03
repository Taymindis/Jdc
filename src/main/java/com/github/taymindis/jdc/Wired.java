package com.github.taymindis.jdc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class Wired {

    private Object ctx;
    private Class<?> clz;
    private String key;
    private String classPath;

    public Wired(Object ctx, Class<?> clz, String key, String classPath) {
        this.ctx = ctx;
        this.clz = clz;
        this.key = key;
        this.classPath = classPath;
    }

    protected Class<?> getClz() {
        return clz;
    }

    protected String getKey() {
        return key;
    }

    protected Object getCtx() {
        return this.ctx;
    }

    protected void setCtx(Object ctx) {
        this.ctx = ctx;
    }

    protected void setClz(Class<?> clz) {
        this.clz = clz;
    }

    protected void setKey(String key) {
        this.key = key;
    }

    protected String getClassPath() {
        return classPath;
    }

    public <T> T call(Object ...args) {
        return call("call", args);
    }

    public <T> T call(String methodName, Object ...args) {
        Class<?> clazz[] = new Class[args.length];
        for(int i=0, len= args.length; i< len; i++) {
            clazz[i] =args[i].getClass();
        }
        try {
            Method d = this.clz.getDeclaredMethod(methodName, clazz);
            return (T) d.invoke(this.ctx, args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

}
