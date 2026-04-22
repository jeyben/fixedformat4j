package com.ancientprogramming.fixedformat4j.format.impl;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A classloader that skips parent delegation for a single nominated class, forcing the JVM to
 * define it fresh from the URL array. All other classes are loaded via normal parent delegation.
 *
 * <p>Used by classloader-leak tests to simulate the "library in parent, application record in
 * child" topology that exists in servlet containers and OSGi environments.
 */
class ChildFirstURLClassLoader extends URLClassLoader {

  private final String targetClassName;

  ChildFirstURLClassLoader(URL[] urls, ClassLoader parent, String targetClassName) {
    super(urls, parent);
    this.targetClassName = targetClassName;
  }

  @Override
  protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    if (name.equals(targetClassName)) {
      synchronized (getClassLoadingLock(name)) {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
          c = findClass(name);
        }
        if (resolve) {
          resolveClass(c);
        }
        return c;
      }
    }
    return super.loadClass(name, resolve);
  }
}
