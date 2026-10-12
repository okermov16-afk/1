package org.apache.logging.log4j;
public class LogManager { public static Logger getLogger(String n) { return new Logger() {
    public void error(String m, Object... a) { System.out.println("LOG " + m + java.util.Arrays.toString(a)); }
    public void error(String m, Throwable t) { System.out.println("LOG " + m + " " + t); } }; } }
