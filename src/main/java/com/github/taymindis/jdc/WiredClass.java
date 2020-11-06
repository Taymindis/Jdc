package com.github.taymindis.jdc;

public class WiredClass {

    private String key;
    private Class<?> clazz;
    private String classPath;

    public WiredClass(String key, Class<?> clazz, String classPath) {
        this.key = key;
        this.clazz = clazz;
        this.classPath = classPath;
    }

    protected Class<?> getClazz() {
        return clazz;
    }

    protected void setClazz(Class<?> clazz) {
        this.clazz = clazz;
    }

    protected String getKey() {
        return key;
    }

    protected void setKey(String key) {
        this.key = key;
    }

    protected String getClassPath() {
        return classPath;
    }

    protected void setClassPath(String classPath) {
        this.classPath = classPath;
    }
}
