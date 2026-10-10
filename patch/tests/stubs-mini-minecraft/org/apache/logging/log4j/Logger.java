package org.apache.logging.log4j;
public interface Logger { void info(String s); void info(String s, Object o); void info(String s, Object a, Object b); void warn(String s); void warn(String s, Object o); void warn(String s, Object a, Object b); void error(String s); void error(String s, Throwable t); void error(String s, Object o); }
