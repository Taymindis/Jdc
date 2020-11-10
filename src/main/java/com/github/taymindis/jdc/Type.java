package com.github.taymindis.jdc;

public class Type<T> {
    T t;

    public Type(T loopHole){
        t = loopHole;
    }

    public Class<T> getType() {
        return (Class<T>) t.getClass();
    }
}
