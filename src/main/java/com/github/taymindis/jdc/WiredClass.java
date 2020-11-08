package com.github.taymindis.jdc;

public class WiredClass {

    private String key;
    private Class<?> clazz;
    private Class<?> proxyClass;

    public WiredClass(String key, Class<?> clazz) {
        this.key = key;
        this.clazz = clazz;
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

    protected Class<?> getProxyClass() {
        return proxyClass;
    }

    protected void setProxyClass(Class<?> proxyClass) {
        this.proxyClass = proxyClass;
    }
}
