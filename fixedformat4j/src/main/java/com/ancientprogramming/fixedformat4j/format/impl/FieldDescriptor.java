package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * Immutable bundle of all per-field metadata computed once per class and cached for reuse across
 * every {@code load()} and {@code export()} call.
 *
 * <p>For repeating fields ({@code count > 1}), {@link #context}, {@link #formatInstructions}, and
 * {@link #formatter} are {@code null} — the runtime delegates to {@link RepeatingFieldSupport}.
 * For fields whose type is itself a {@code @Record}, {@link #formatter} is {@code null} and the
 * runtime recurses into {@code FixedFormatManagerImpl}.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.0
 */
class FieldDescriptor {

  final AnnotationTarget target;
  final Method setter;
  final MethodHandle setterHandle;
  final Field fieldAnnotation;
  final Class<?> datatype;
  final FormatContext<?> context;
  final FormatInstructions formatInstructions;
  final FixedFormatter<?> formatter;
  final boolean isRepeating;
  final boolean isNestedRecord;
  /**
   * {@code true} when this descriptor should participate in {@code load()} (i.e. its parsed value
   * is written to the POJO via the setter). For plain {@code @Field} annotations this is always
   * {@code true}. For {@code @Fields}, only the first annotation in the array is a load field;
   * the remainder are export-only.
   */
  final boolean isLoadField;

  FieldDescriptor(
      AnnotationTarget target,
      Method setter,
      MethodHandle setterHandle,
      Field fieldAnnotation,
      Class<?> datatype,
      FormatContext<?> context,
      FormatInstructions formatInstructions,
      FixedFormatter<?> formatter,
      boolean isRepeating,
      boolean isNestedRecord,
      boolean isLoadField) {
    this.target = target;
    this.setter = setter;
    this.setterHandle = setterHandle;
    this.fieldAnnotation = fieldAnnotation;
    this.datatype = datatype;
    this.context = context;
    this.formatInstructions = formatInstructions;
    this.formatter = formatter;
    this.isRepeating = isRepeating;
    this.isNestedRecord = isNestedRecord;
    this.isLoadField = isLoadField;
  }
}
