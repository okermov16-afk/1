package org.lwjgl.input;
public class Keyboard { public static final java.util.Set<Integer> down = new java.util.HashSet<>(); public static boolean isKeyDown(int k) { return down.contains(k); } }
