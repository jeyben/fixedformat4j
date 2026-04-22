package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Verifies that the three JVM-level caches (ClassMetadataCache, AbstractPatternFormatter,
 * FixedFormatManagerImpl.VALIDATED_CLASSES) do not pin a record class's defining ClassLoader
 * after all other strong references to that class are released.
 *
 * <p>The test simulates the "library in parent, application record in child" classloader topology
 * that exists in servlet containers, OSGi, and Spring Boot DevTools. It fails if any cache holds
 * a strong reference to the child-loaded record class (and therefore to its ClassLoader).
 */
class TestClassloaderLeak {

  @Test
  @SuppressWarnings("unchecked")
  void cachedRecordClassIsGcEligibleAfterLoaderRelease() throws Exception {
    // Locate test-classes directory so ChildFirstURLClassLoader can find IsolatedRecord.class.
    URL testClassesUrl = IsolatedRecord.class.getProtectionDomain().getCodeSource().getLocation();

    WeakReference<ClassLoader> loaderRef;
    {
      // Simulate child classloader (e.g. a webapp classloader) that loads IsolatedRecord
      // independently of the parent, which owns the fixedformat4j library.
      ChildFirstURLClassLoader loader = new ChildFirstURLClassLoader(
          new URL[]{testClassesUrl},
          FixedFormatManagerImpl.class.getClassLoader(),
          IsolatedRecord.class.getName());

      Class<Object> recordCls = (Class<Object>) loader.loadClass(IsolatedRecord.class.getName());

      // Sanity check: the record class must have been defined by the child, not the parent.
      assertSame(loader, recordCls.getClassLoader(),
          "IsolatedRecord should be defined by the child classloader");
      assertNotSame(IsolatedRecord.class, recordCls,
          "child-loaded IsolatedRecord must be a distinct Class object from the parent-loaded one");

      // Warm up all three caches.
      FixedFormatManager mgr = new FixedFormatManagerImpl();
      Object loaded = mgr.load(recordCls, "hello     ");
      mgr.export(loaded);

      loaderRef = new WeakReference<>(loader);
      // Drop all strong references to the child-domain objects.
      // After this block exits, only the WeakReference remains.
    }

    // Repeatedly hint at GC. ClassValue-based caches should allow the loader to be collected.
    for (int i = 0; i < 20 && loaderRef.get() != null; i++) {
      System.gc();
      Thread.sleep(50);
    }

    assertNull(loaderRef.get(),
        "fixedformat4j caches retained the child classloader after all strong references were released. "
            + "This indicates a classloader leak — the caches hold a strong reference to the "
            + "child-loaded record Class, which pins the child ClassLoader and all classes it defined.");
  }
}
