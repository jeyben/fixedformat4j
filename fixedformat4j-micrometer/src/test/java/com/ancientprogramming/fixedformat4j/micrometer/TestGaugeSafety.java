package com.ancientprogramming.fixedformat4j.micrometer;

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Safety properties of the fixedformat.metadata.cache.classes gauge: the class-tracking set
 * must not pin record classes (and thereby their classloaders — the exact leak the core
 * ClassValue caches are designed to avoid), and each instrumented manager must keep its own
 * working gauge even when several share one registry.
 */
class TestGaugeSafety {

  private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

  @Test
  void trackedRecordClassesDoNotPinTheirClassloader() throws Exception {
    FixedFormatManager manager = FixedFormatMetrics.of(registry).instrument(new FixedFormatManagerImpl());

    WeakReference<ClassLoader> loaderRef = loadThroughChildLoaderAndRelease(manager);

    for (int i = 0; i < 20 && loaderRef.get() != null; i++) {
      System.gc();
      Thread.sleep(50);
    }

    assertNull(loaderRef.get(),
        "the instrumented manager retained the child classloader after all strong references "
            + "were released — the gauge's class-tracking set must hold record classes weakly, "
            + "otherwise the long-lived manager pins every classloader it ever saw");
  }

  /**
   * Extracted so the stack frame (and all its locals) is popped before the GC loop runs;
   * only the long-lived manager survives — exactly the production topology.
   */
  @SuppressWarnings("unchecked")
  private WeakReference<ClassLoader> loadThroughChildLoaderAndRelease(FixedFormatManager manager)
      throws Exception {
    URL testClassesUrl = BasicRecord.class.getProtectionDomain().getCodeSource().getLocation();
    ChildFirstURLClassLoader loader = new ChildFirstURLClassLoader(
        new URL[]{testClassesUrl},
        FixedFormatManagerImpl.class.getClassLoader(),
        BasicRecord.class.getName());

    Class<Object> childRecordClass = (Class<Object>) loader.loadClass(BasicRecord.class.getName());
    assertSame(loader, childRecordClass.getClassLoader());

    manager.load(childRecordClass, "some text 00042");

    return new WeakReference<>(loader);
  }

  @Test
  void eachInstrumentedManagerOnASharedRegistryKeepsItsOwnGauge() {
    FixedFormatMetrics metrics = FixedFormatMetrics.of(registry);
    FixedFormatManager first = metrics.instrument(new FixedFormatManagerImpl());
    FixedFormatManager second = metrics.instrument(new FixedFormatManagerImpl());

    first.load(BasicRecord.class, "some text 00042");
    second.load(TestMeteredManager.OtherRecord.class, "x");

    Collection<Gauge> gauges = registry.find("fixedformat.metadata.cache.classes").gauges();
    assertEquals(2, gauges.size(),
        "each manager must register its own gauge; without a distinguishing tag Micrometer "
            + "silently drops the second registration and never polls its class set");
    for (Gauge gauge : gauges) {
      assertEquals(1.0, gauge.value(), "each manager has seen exactly one distinct class");
    }
  }
}
