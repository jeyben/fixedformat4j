/*
 * Copyright 2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancientprogramming.fixedformat4j.processor;

import com.ancientprogramming.fixedformat4j.annotation.Field;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeMirror;
import java.lang.annotation.Annotation;
import java.util.List;

/**
 * One {@code @Field}/{@code @Fields}-annotated member of a record class, as seen by the
 * mirror API: the annotated element (getter method, record accessor, or backing field), the
 * field's datatype and the {@code @Field} annotation instances declared on it.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
final class AnnotatedFixedFormatField {

  final Element element;
  final TypeMirror datatype;
  final List<Field> fieldAnnotations;

  AnnotatedFixedFormatField(Element element, TypeMirror datatype, List<Field> fieldAnnotations) {
    this.element = element;
    this.datatype = datatype;
    this.fieldAnnotations = fieldAnnotations;
  }

  <A extends Annotation> A supplementaryAnnotation(Class<A> annotationType) {
    return element.getAnnotation(annotationType);
  }

  String label() {
    String member = element.getSimpleName().toString();
    String suffix = element.getKind() == ElementKind.METHOD ? "()" : "";
    return element.getEnclosingElement().toString() + "." + member + suffix;
  }
}
