package com.github.taymindis.jdc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class WiredContext {
    private Object ctx;
    private boolean _wiringjdc_;
    private Class<?> classUsing;
    public WiredContext() {
        _wiringjdc_ = false;
    }

    public WiredContext(Object ctx, Class<?> classUsing) {
        this.ctx = ctx;
        this._wiringjdc_ = false;
        this.classUsing = classUsing;
    }

    protected Object getCtx() {
        return this.ctx;
    }

    protected void setCtx(Object ctx) {
        this.ctx = ctx;
    }

    protected void setClassUsing(Class<?> classUsing) {
        this.classUsing = classUsing;
    }

    protected boolean is_wiringjdc_() {
        return _wiringjdc_;
    }

    protected void set_wiringjdc_(boolean _wiringjdc_) {
        this._wiringjdc_ = _wiringjdc_;
    }

    protected <T> T invoke(String uniqMethodName, boolean isStatic, Class<?>[] classes, Object[] args) {
//        Class<?> clazz[] = new Class[args.length];
//        for (int i = 0, len = args.length; i < len; i++) {
//            clazz[i] = args[i] != null ?  args[i].getClass() : new Object().getClass();
//        }
        try {
            Method d = this.classUsing.getMethod(uniqMethodName, classes);
            if(isStatic) {
                return (T) d.invoke(null, args);
            }
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

//    protected static <T> T invokeStatic(String methodName, Class<?> staticClazz, Object[] args) {
//        Class<?> clazz[] = new Class[args.length];
//        for (int i = 0, len = args.length; i < len; i++) {
//            clazz[i] = args[i].getClass();
//        }
//        try {
//            WiredContext wiredContext = Jdc.retrieve(generateKey(staticClazz));
//
//            Method d = this.wiredClass.getDeclaredMethod(methodName, clazz);
//            return (T) d.invoke(null, args);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        } catch (IllegalAccessException e) {
//            e.printStackTrace();
//        } catch (InvocationTargetException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//

}
