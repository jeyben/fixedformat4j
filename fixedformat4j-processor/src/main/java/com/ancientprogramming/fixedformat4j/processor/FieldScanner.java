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
import com.ancientprogramming.fixedformat4j.annotation.Fields;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Mirror-API counterpart of the runtime {@code AnnotationScanner}: discovers all
 * {@code @Field}/{@code @Fields} targets on a record class, walking the superclass hierarchy,
 * with field annotations taking priority over method annotations for the same property.
 *
 * <p>For Java {@code record} types only accessor methods are scanned: a component annotation
 * propagates to both the backing field and the accessor, so scanning fields would double-count
 * every field and break the cross-field checks.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
final class FieldScanner {

  private FieldScanner() {
  }

  static List<AnnotatedFixedFormatField> scan(TypeElement recordType) {
    LinkedHashMap<String, AnnotatedFixedFormatField> targets = new LinkedHashMap<>();
    boolean isJavaRecord = isJavaRecord(recordType);

    for (TypeElement type : hierarchyOf(recordType)) {
      for (Element enclosed : type.getEnclosedElements()) {
        if (enclosed.getKind() != ElementKind.METHOD || !enclosed.getModifiers().contains(Modifier.PUBLIC)) {
          continue;
        }
        List<Field> annotations = fieldAnnotationsOn(enclosed);
        if (annotations.isEmpty()) {
          continue;
        }
        String name = enclosed.getSimpleName().toString();
        if (!isJavaRecord && !name.startsWith("get") && !name.startsWith("is")) {
          continue;
        }
        String key = isJavaRecord ? name : stripMethodPrefix(name);
        TypeMirror datatype = ((ExecutableElement) enclosed).getReturnType();
        targets.putIfAbsent(key, new AnnotatedFixedFormatField(enclosed, datatype, annotations));
      }
    }

    if (isJavaRecord) {
      return new ArrayList<>(targets.values());
    }

    for (TypeElement type : hierarchyOf(recordType)) {
      for (Element enclosed : type.getEnclosedElements()) {
        if (enclosed.getKind() != ElementKind.FIELD) {
          continue;
        }
        List<Field> annotations = fieldAnnotationsOn(enclosed);
        if (annotations.isEmpty()) {
          continue;
        }
        String key = capitalize(enclosed.getSimpleName().toString());
        // put, not putIfAbsent: field annotations override method annotations for the same
        // property. Within this pass it also means a superclass field annotation overwrites a
        // subclass one — matching the runtime AnnotationScanner, which puts in the same order.
        targets.put(key, new AnnotatedFixedFormatField(enclosed, enclosed.asType(), annotations));
      }
    }

    return new ArrayList<>(targets.values());
  }

  static boolean isJavaRecord(TypeElement type) {
    return "RECORD".equals(type.getKind().name());
  }

  private static List<Field> fieldAnnotationsOn(Element element) {
    Field single = element.getAnnotation(Field.class);
    if (single != null) {
      return List.of(single);
    }
    Fields multiple = element.getAnnotation(Fields.class);
    if (multiple != null) {
      return Arrays.asList(multiple.value());
    }
    return List.of();
  }

  private static List<TypeElement> hierarchyOf(TypeElement type) {
    List<TypeElement> hierarchy = new ArrayList<>();
    TypeElement current = type;
    while (current != null && !"java.lang.Object".contentEquals(current.getQualifiedName())) {
      hierarchy.add(current);
      TypeMirror superclass = current.getSuperclass();
      current = superclass.getKind() == TypeKind.DECLARED
          ? (TypeElement) ((DeclaredType) superclass).asElement()
          : null;
    }
    return hierarchy;
  }

  private static String stripMethodPrefix(String name) {
    if (name.startsWith("get")) {
      return name.substring(3);
    }
    if (name.startsWith("is")) {
      return name.substring(2);
    }
    return name;
  }

  private static String capitalize(String name) {
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }
}
