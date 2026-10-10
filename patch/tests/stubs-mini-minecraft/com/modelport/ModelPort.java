package com.modelport;
import org.apache.logging.log4j.Logger;
public class ModelPort {
    public static final java.util.List<String> LINES = new java.util.ArrayList<>();
    public static final Logger LOG = (Logger) java.lang.reflect.Proxy.newProxyInstance(ModelPort.class.getClassLoader(), new Class<?>[]{Logger.class}, (p, m, a) -> { LINES.add(m.getName() + ":" + java.util.Arrays.toString(a)); return null; });
}
