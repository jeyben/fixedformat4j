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

import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatEnum;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * Per-field checks. Each check is the mirror-API twin of a runtime check in
 * {@code com.ancientprogramming.fixedformat4j.format.impl.FieldValidator} (and
 * {@code PatternValidator}); when a check changes there, it must change here too.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
final class FieldChecker {

  private final Messager messager;

  FieldChecker(Messager messager) {
    this.messager = messager;
  }

  /**
   * Twin of {@code FieldValidator.doValidateFieldPattern} + {@code PatternValidator}: an
   * explicit {@code @FixedFormatPattern} on a date/time-typed field must be constructible.
   * Absent annotations fall back to library defaults, which are valid by construction.
   */
  void checkPattern(AnnotatedFixedFormatField target) {
    FixedFormatPattern patternAnnotation = target.supplementaryAnnotation(FixedFormatPattern.class);
    if (patternAnnotation == null) {
      return;
    }
    String pattern = patternAnnotation.value();
    String datatype = qualifiedNameOf(target.datatype);
    if ("java.util.Date".equals(datatype)) {
      try {
        new SimpleDateFormat(pattern);
      } catch (IllegalArgumentException e) {
        error(target, format("Invalid date pattern '%s' on %s: %s", pattern, target.label(), e.getMessage()));
      }
    } else if ("java.time.LocalDate".equals(datatype) || "java.time.LocalDateTime".equals(datatype)) {
      try {
        DateTimeFormatter.ofPattern(pattern);
      } catch (IllegalArgumentException e) {
        error(target, format("Invalid date/time pattern '%s' on %s: %s", pattern, target.label(), e.getMessage()));
      }
    }
  }

  /**
   * Twin of {@code FieldValidator.doValidateEnumFieldLength}: the widest serialized form of an
   * enum field ({@code Enum#name()} for LITERAL, the digit width of the highest ordinal for
   * NUMERIC) must fit in {@code @Field(length)}. Skipped for REST_OF_LINE fields.
   */
  void checkEnumLength(AnnotatedFixedFormatField target, Field fieldAnnotation) {
    if (fieldAnnotation.length() == Field.REST_OF_LINE) {
      return;
    }
    if (target.datatype.getKind() != TypeKind.DECLARED) {
      return;
    }
    Element datatypeElement = ((DeclaredType) target.datatype).asElement();
    if (datatypeElement.getKind() != ElementKind.ENUM) {
      return;
    }
    List<String> constants = datatypeElement.getEnclosedElements().stream()
        .filter(enclosed -> enclosed.getKind() == ElementKind.ENUM_CONSTANT)
        .map(enclosed -> enclosed.getSimpleName().toString())
        .collect(Collectors.toList());
    if (constants.isEmpty()) {
      return;
    }
    FixedFormatEnum enumAnnotation = target.supplementaryAnnotation(FixedFormatEnum.class);
    EnumFormat enumFormat = (enumAnnotation != null) ? enumAnnotation.value() : EnumFormat.LITERAL;
    int maxLength;
    if (enumFormat == EnumFormat.NUMERIC) {
      maxLength = String.valueOf(constants.size() - 1).length();
    } else {
      maxLength = constants.stream().mapToInt(String::length).max().orElse(0);
    }
    if (maxLength > fieldAnnotation.length()) {
      error(target, format("Enum [%s] has values with max length %d, which exceeds @Field length %d on %s",
          ((TypeElement) datatypeElement).getQualifiedName(), maxLength, fieldAnnotation.length(), target.label()));
    }
  }

  static String qualifiedNameOf(TypeMirror type) {
    if (type.getKind() == TypeKind.DECLARED) {
      return ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString();
    }
    return type.toString();
  }

  private void error(AnnotatedFixedFormatField target, String message) {
    messager.printMessage(Diagnostic.Kind.ERROR, message, target.element);
  }
}
