package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Reflective access to the Java {@code record} API (JDK 16+) from bytecode compiled at
 * release 11.
 *
 * <p>A record class can only be loaded by a JVM that has the record API, so whenever
 * {@link #isJavaRecord} returns {@code true} the reflective lookups below are guaranteed
 * to resolve.
 *
 * <p>Performance: these lookups run once per record class, inside the
 * {@link ClassMetadataCache} build (guarded by {@link ClassValue}); the per-{@code load()}
 * hot path only sees the cached {@link ConstructorBinding}.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
final class JavaRecordSupport {

  private JavaRecordSupport() {
  }

  /**
   * Returns whether the given class is a Java {@code record}. Checked via the superclass
   * name because {@code java.lang.Record} and {@code Class.isRecord()} are not referenceable
   * at release 11; the compiler forbids extending {@code java.lang.Record} explicitly, so the
   * check is equivalent for loadable classes.
   */
  static boolean isJavaRecord(Class<?> clazz) {
    Class<?> superclass = clazz.getSuperclass();
    return superclass != null && "java.lang.Record".equals(superclass.getName());
  }

  /** Name and type of one record component, in declaration order. */
  static final class ComponentInfo {
    final String name;
    final Class<?> type;

    ComponentInfo(String name, Class<?> type) {
      this.name = name;
      this.type = type;
    }
  }

  /**
   * Returns the record components of the given record class in declaration order — the
   * exact parameter list of the canonical constructor.
   */
  static List<ComponentInfo> components(Class<?> recordClass) {
    try {
      Object[] components = (Object[]) Class.class.getMethod("getRecordComponents").invoke(recordClass);
      List<ComponentInfo> result = new ArrayList<>(components.length);
      for (Object component : components) {
        String name = (String) component.getClass().getMethod("getName").invoke(component);
        Class<?> type = (Class<?>) component.getClass().getMethod("getType").invoke(component);
        result.add(new ComponentInfo(name, type));
      }
      return result;
    } catch (ReflectiveOperationException e) {
      throw new FixedFormatException(
          format("unable to read record components of %s", recordClass.getName()), e);
    }
  }
}
