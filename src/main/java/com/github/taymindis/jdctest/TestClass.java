package com.github.taymindis.jdctest;

import com.github.taymindis.jdc.WiredBean;


public class TestClass {

    public static void main(String ...args) {
        Integer a= null;
        new WiredBean().issue("callMe",
                a, String.class,
                123, Integer.class);
        new WiredBean().callMe(a, 123);
    }
}
