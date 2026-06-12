package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Immutable binding of parsed field values to the canonical constructor of a Java
 * {@code record}: the constructor {@link MethodHandle} plus a precomputed map from each
 * load {@link FieldDescriptor} to its parameter position (component accessor name equals
 * canonical-constructor parameter name).
 *
 * <p>Built once per record class and cached by {@link ClassMetadataCache}. The
 * per-{@code load()} cost is one {@code Object[]} allocation and a single constructor
 * invoke through the handle — the same invocation mechanism the setter path uses.
 *
 * <p>A load descriptor with no matching component (e.g. an annotated derived accessor
 * declared in the record body) is left unmapped and its parsed value is dropped — the same
 * semantics as a {@code @Field} getter without a setter on a conventional POJO.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
final class ConstructorBinding {

  private final MethodHandle constructorHandle;
  private final Object[] defaults;
  private final Map<FieldDescriptor, Integer> parameterIndexByDescriptor;

  private ConstructorBinding(MethodHandle constructorHandle, Object[] defaults,
                             Map<FieldDescriptor, Integer> parameterIndexByDescriptor) {
    this.constructorHandle = constructorHandle;
    this.defaults = defaults;
    this.parameterIndexByDescriptor = parameterIndexByDescriptor;
  }

  static ConstructorBinding forRecord(Class<?> recordClass, List<FieldDescriptor> descriptors) {
    List<JavaRecordSupport.ComponentInfo> components = JavaRecordSupport.components(recordClass);

    Class<?>[] parameterTypes = new Class<?>[components.size()];
    Object[] defaults = new Object[components.size()];
    Map<String, Integer> indexByComponentName = new HashMap<>(components.size() * 2);
    for (int i = 0; i < components.size(); i++) {
      JavaRecordSupport.ComponentInfo component = components.get(i);
      parameterTypes[i] = component.type;
      defaults[i] = primitiveDefault(component.type);
      indexByComponentName.put(component.name, i);
    }

    Map<FieldDescriptor, Integer> parameterIndexByDescriptor = new HashMap<>(descriptors.size() * 2);
    for (FieldDescriptor desc : descriptors) {
      Integer index = indexByComponentName.get(desc.target.getter.getName());
      if (desc.isLoadField && index != null) {
        parameterIndexByDescriptor.put(desc, index);
      }
    }

    return new ConstructorBinding(canonicalConstructorHandle(recordClass, parameterTypes),
        defaults, parameterIndexByDescriptor);
  }

  /** Returns a fresh argument array pre-filled with primitive defaults ({@code 0}, {@code false}, …). */
  Object[] newArgs() {
    return defaults.clone();
  }

  /**
   * Writes a parsed value into the argument slot bound to the given descriptor. A
   * {@code null} value or an unbound descriptor leaves the slot at its default — mirroring
   * the setter path, which skips the setter in both cases.
   */
  void assign(FieldDescriptor desc, Object value, Object[] args) {
    Integer index = parameterIndexByDescriptor.get(desc);
    if (index != null && value != null) {
      args[index] = value;
    }
  }

  Object newInstance(Class<?> recordClass, Object[] args) {
    try {
      return constructorHandle.invokeWithArguments(args);
    } catch (Throwable e) {
      throw new FixedFormatException(
          format("unable to create instance of %s through its canonical constructor", recordClass.getName()), e);
    }
  }

  private static MethodHandle canonicalConstructorHandle(Class<?> recordClass, Class<?>[] parameterTypes) {
    try {
      Constructor<?> canonical = recordClass.getDeclaredConstructor(parameterTypes);
      return MethodHandles.lookup().unreflectConstructor(canonical);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new FixedFormatException(
          format("unable to access the canonical constructor of %s", recordClass.getName()), e);
    }
  }

  private static Object primitiveDefault(Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (type == boolean.class) return Boolean.FALSE;
    if (type == char.class) return (char) 0;
    if (type == byte.class) return (byte) 0;
    if (type == short.class) return (short) 0;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == float.class) return 0F;
    return 0D;
  }
}
