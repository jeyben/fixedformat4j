package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Fields;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.getFixedFormatterInstance;

/**
 * JVM-level cache of per-class field metadata ({@link FieldDescriptor} lists).
 *
 * <p>The first call to {@link #get} for a class scans its annotations and builds one
 * {@link FieldDescriptor} per effective {@code @Field}. Subsequent calls return the same
 * immutable list without re-scanning.
 *
 * <p>Thread safety: {@code computeIfAbsent} guarantees that {@link #build} runs at most once per
 * class key. Helper objects ({@link AnnotationScanner}, {@link FormatInstructionsBuilder},
 * {@link RepeatingFieldSupport}) are created as local variables inside {@code build} so that
 * concurrent builds of different classes never share mutable state.
 *
 * <p><strong>Note:</strong> this cache is never cleared. In multi-classloader environments
 * (e.g. application servers with hot-reload, OSGi containers) old {@link Class} references may
 * be retained here after their classloader is discarded, preventing garbage collection.
 * In such environments consider using a {@link java.lang.ref.WeakReference}-based map instead.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.0
 */
class ClassMetadataCache {

  static final ClassMetadataCache INSTANCE = new ClassMetadataCache();

  private final Map<Class<?>, List<FieldDescriptor>> cache = new ConcurrentHashMap<>();

  List<FieldDescriptor> get(Class<?> clazz) {
    return cache.computeIfAbsent(clazz, this::build);
  }

  private List<FieldDescriptor> build(Class<?> clazz) {
    AnnotationScanner scanner = new AnnotationScanner();
    FormatInstructionsBuilder instructionsBuilder = new FormatInstructionsBuilder();
    RepeatingFieldSupport repeatingFieldSupport = new RepeatingFieldSupport();

    List<FieldDescriptor> result = new ArrayList<>();
    for (AnnotationTarget target : scanner.scan(clazz)) {
      Field fieldAnnotation = target.annotationSource.getAnnotation(Field.class);
      Fields fieldsAnnotation = target.annotationSource.getAnnotation(Fields.class);
      if (fieldAnnotation != null) {
        result.add(buildDescriptor(clazz, target, fieldAnnotation, true, scanner, instructionsBuilder, repeatingFieldSupport));
      } else if (fieldsAnnotation != null) {
        Field[] fields = fieldsAnnotation.value();
        for (int i = 0; i < fields.length; i++) {
          result.add(buildDescriptor(clazz, target, fields[i], i == 0, scanner, instructionsBuilder, repeatingFieldSupport));
        }
      }
    }
    return Collections.unmodifiableList(result);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private FieldDescriptor buildDescriptor(
      Class<?> clazz,
      AnnotationTarget target,
      Field fieldAnnotation,
      boolean isLoadField,
      AnnotationScanner scanner,
      FormatInstructionsBuilder instructionsBuilder,
      RepeatingFieldSupport repeatingFieldSupport) {

    Class<?> datatype = instructionsBuilder.datatype(target.getter, fieldAnnotation);
    repeatingFieldSupport.validateCount(target.getter, fieldAnnotation);
    boolean isRepeating = fieldAnnotation.count() > 1;
    boolean hasCustomFormatter = fieldAnnotation.formatter() != ByTypeFormatter.class;
    boolean isNestedRecord = !isRepeating && !hasCustomFormatter && datatype.getAnnotation(Record.class) != null;

    FormatContext<?> context = isRepeating ? null : instructionsBuilder.context(datatype, fieldAnnotation);
    FormatInstructions formatInstructions = isRepeating ? null : instructionsBuilder.build(target.annotationSource, fieldAnnotation, datatype, clazz);
    FixedFormatter<?> formatter = (isRepeating || isNestedRecord) ? null
        : getFixedFormatterInstance(context.getFormatter(), context);

    Method setter = resolveSetter(clazz, target.getter, datatype, scanner);
    MethodHandle setterHandle = toHandle(setter);

    return new FieldDescriptor(target, setter, setterHandle, fieldAnnotation, datatype, context,
        formatInstructions, formatter, isRepeating, isNestedRecord, isLoadField);
  }

  private Method resolveSetter(Class<?> clazz, Method getter, Class<?> datatype, AnnotationScanner scanner) {
    String setterName = "set" + scanner.stripMethodPrefix(getter.getName());
    try {
      return clazz.getMethod(setterName, datatype);
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  private MethodHandle toHandle(Method method) {
    if (method == null) return null;
    try {
      return MethodHandles.lookup().unreflect(method);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Cannot create MethodHandle for " + method, e);
    }
  }
}
