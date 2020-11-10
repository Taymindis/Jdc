package com.github.taymindis.jdc;


import java.lang.reflect.Method;

public class WiredBean {

    public void callMe(String a) {
        System.out.println("ASDASDASDASDSD");
    }

    public void callMe(String a, Integer b) {
        System.out.println("ASD ASDASD");
    }

    public void callMe(Integer a, Integer b) {
        System.out.println("INTER ASDASD");
    }

    public <T> T issue(String methodName, Object... args) {
        try {
            for (Method m : this.getClass().getMethods()) {
                if (m.getName().equals(methodName)) {
                    Class<?>[] params = m.getParameterTypes();
                    if (args == null || args.length == params.length) {
                        return (T) m.invoke(this, args);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
