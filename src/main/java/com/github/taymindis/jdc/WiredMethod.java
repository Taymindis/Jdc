package com.github.taymindis.jdc;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WiredMethod {
    private Object ctx;
    private Class<?> classUsing;

    WiredMethod(){

    }

    public <T> T send2(String commandName, Object[] args) {
        try {
            for (Method m : this.getClass().getMethods()) {
                WiredCommand wiredCommand = m.getAnnotation(WiredCommand.class);
                if (wiredCommand != null) {
                    String wiredCommandName = wiredCommand.value();
                    if (wiredCommandName.equals(commandName)) {
                        return (T) m.invoke(this, args);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }


    public <T> T send(String commandName, Object... args) {
        return send2(commandName, args);
    }


    public Object getCtx() {
        return ctx;
    }

    public void setCtx(Object ctx) {
        this.ctx = ctx;
    }

    public Class<?> getClassUsing() {
        return classUsing;
    }

    public void setClassUsing(Class<?> classUsing) {
        this.classUsing = classUsing;
    }
}
