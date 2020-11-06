package com.github.taymindis.jdctest;


import com.github.taymindis.jdc.Jdc;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class TestClass {

    public static void main(String ...args) throws IllegalAccessException, IOException, InvocationTargetException, InterruptedException {
//        TestBean testBean = new TestBean();


        for(int i = 0; i< 30 ; i++ ) {
            TestBean bean = Jdc.wireBean(TestBean.class);
            bean.execute(null, 1);
            Thread.sleep(1500);
        }
        System.out.println("ASDASDASD");
    }
}
