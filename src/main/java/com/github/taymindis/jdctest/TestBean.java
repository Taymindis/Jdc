package com.github.taymindis.jdctest;

import com.github.taymindis.jdc.SkipWire;
import com.github.taymindis.jdc.Wired;

import java.io.Serializable;

@Wired
public class TestBean implements Serializable {

    public String myMethod(String abc, Integer a) {
        return "com.github.taymindis.jdctest.TestBean.............";
    }

    public void execute(String abc, Integer a) {
        System.out.println("ASDASDASDASD");
    }

    public static void staticCall(String abc, Integer a) {
        System.out.println("ASDASASDASDASDASDASDASD");
    }


}
