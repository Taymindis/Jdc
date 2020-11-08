package com.github.taymindis.jdctest;

import com.github.taymindis.jdc.SkipWire;
import com.github.taymindis.jdc.Wired;
import com.github.taymindis.jdc.WiredCommand;

import java.io.Serializable;

@Wired
public class TestBean implements Serializable, WiredCommand {

    public String myMethod(String abc, Integer a)  {
        return "TestBean.............";
    }
    public void execute(String abc, Integer a)  {
         System.out.println("ASDASDASDASD");
    }
    public static void staticCall(String abc, Integer a)  {
         System.out.println("ASDASASDASDASDASDASDASD");
    }

    @Override
    public <T> T execute(String commandName, Object... args) {
        switch (commandName) {
            case "ABC":


                break;
            default:
        }


        return null;
    }

    public <T extends String> T execute2(String commandName, Object... args) {
        switch (commandName) {
            case "ABC":


                break;
            default:
        }


        return null;
    }

    @SkipWire
    public Class<?> exeqcute(String commandName, Object... args) {
        switch (commandName) {
            case "ABC":


                break;
            default:
        }


        return null;
    }


}
