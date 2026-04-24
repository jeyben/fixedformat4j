package com.ancientprogramming.fixedformat4j.format.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import static java.lang.String.format;

/**
 * Pairs a getter {@link Method} (used for invocation and type resolution) with an
 * {@link AnnotatedElement} (used for supplementary annotation lookup). When the
 * {@code @Field} annotation is on the getter, both references point to the same object.
 * When it is on a Java field, they differ.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.6.0
 */
class AnnotationTarget {

  final Method getter;
  final AnnotatedElement annotationSource;
  final MethodHandle getterHandle;

  private AnnotationTarget(Method getter, AnnotatedElement annotationSource) {
    this.getter = getter;
    this.annotationSource = annotationSource;
    try {
      this.getterHandle = MethodHandles.lookup().unreflect(getter);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(format("Cannot create MethodHandle for %s", getter), e);
    }
  }

  /** Annotation is on the getter — getter serves as both invoker and annotation source. */
  static AnnotationTarget ofMethod(Method method) {
    return new AnnotationTarget(method, method);
  }

  /** Annotation is on a Java field — getter is derived, field is the annotation source. */
  static AnnotationTarget ofField(Method getter, java.lang.reflect.Field field) {
    return new AnnotationTarget(getter, field);
  }
}
