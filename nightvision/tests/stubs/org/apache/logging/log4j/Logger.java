package org.apache.logging.log4j;
public interface Logger { void error(String m, Object... a); void error(String m, Throwable t); }
