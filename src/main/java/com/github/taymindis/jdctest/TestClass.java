package com.github.taymindis.jdctest;



import com.github.taymindis.jdc.Jdc;

import java.io.IOException;

public class TestClass {

    public static void main(String ...args) throws IllegalAccessException, IOException, InterruptedException {

        for(;;) {
            TestBean a = Jdc.wireBean(TestBean.class);
            a.execute("ASdadasdasd", 123);
            Thread.sleep(1500);
            Jdc.hotReload();
        }
//       System.out.println(a.myMethod("1212", 123));
    }
}
