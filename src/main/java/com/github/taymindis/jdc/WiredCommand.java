package com.github.taymindis.jdc;

import java.io.Serializable;

public interface WiredCommand extends Serializable {
   <T> T execute(String commandName, Object ...args);
}
