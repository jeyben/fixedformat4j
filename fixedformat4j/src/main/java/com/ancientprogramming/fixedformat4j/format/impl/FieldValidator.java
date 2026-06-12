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
package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatEnum;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;

import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

class FieldValidator {

  private FieldValidator() {}

  @SuppressWarnings({"unchecked", "rawtypes"})
  static void doValidateEnumFieldLength(AnnotationTarget target, Field fieldAnnotation) {
    if (fieldAnnotation.length() == Field.REST_OF_LINE) return;
    FormatInstructionsBuilder instructionsBuilder = new FormatInstructionsBuilder();
    Class<?> datatype = instructionsBuilder.datatype(target.getter, fieldAnnotation);
    if (!datatype.isEnum()) {
      return;
    }
    Enum<?>[] constants = (Enum<?>[]) datatype.getEnumConstants();
    if (constants == null || constants.length == 0) {
      return;
    }
    FixedFormatEnum enumAnnotation = target.annotationSource.getAnnotation(FixedFormatEnum.class);
    EnumFormat enumFormat = (enumAnnotation != null) ? enumAnnotation.value() : EnumFormat.LITERAL;
    int maxLength;
    if (enumFormat == EnumFormat.NUMERIC) {
      maxLength = String.valueOf(constants.length - 1).length();
    } else {
      maxLength = Arrays.stream(constants)
          .mapToInt(e -> e.name().length())
          .max()
          .orElse(0);
    }
    if (maxLength > fieldAnnotation.length()) {
      throw new FixedFormatException(format(
          "Enum [%s] has values with max length %d, which exceeds @Field length %d on %s.%s()",
          datatype.getName(), maxLength, fieldAnnotation.length(),
          target.getter.getDeclaringClass().getName(), target.getter.getName()));
    }
  }

  static void doValidateRestOfLineField(AnnotationTarget target, Field fieldAnnotation) {
    if (fieldAnnotation.length() != Field.REST_OF_LINE) return;

    FormatInstructionsBuilder instructionsBuilder = new FormatInstructionsBuilder();
    Class<?> datatype = instructionsBuilder.datatype(target.getter, fieldAnnotation);
    String getterRef = target.getter.getDeclaringClass().getName() + "." + target.getter.getName() + "()";

    if (!String.class.equals(datatype)) {
      throw new FixedFormatException(format(
          "@Field(length = -1) is only supported for String fields, but %s returns %s",
          getterRef, datatype.getName()));
    }
    if (fieldAnnotation.count() != 1) {
      throw new FixedFormatException(format(
          "@Field(length = -1) cannot be combined with count > 1 on %s", getterRef));
    }
    if (fieldAnnotation.align() != Align.INHERIT) {
      throw new FixedFormatException(format(
          "@Field(length = -1): 'align' is not applicable when length = -1 on %s", getterRef));
    }
    if (fieldAnnotation.paddingChar() != ' ') {
      throw new FixedFormatException(format(
          "@Field(length = -1): 'paddingChar' is not applicable when length = -1 on %s", getterRef));
    }
    if (fieldAnnotation.nullChar() != Field.UNSET_NULL_CHAR) {
      throw new FixedFormatException(format(
          "@Field(length = -1): 'nullChar' is not applicable when length = -1 on %s", getterRef));
    }
    if (!fieldAnnotation.nullValue().isEmpty()) {
      throw new FixedFormatException(format(
          "@Field(length = -1): 'nullValue' is not applicable when length = -1 on %s", getterRef));
    }
  }

  static void doValidateRestOfLineIsLastField(Class<?> clazz, List<FieldDescriptor> descriptors) {
    int restOfLineOffset = -1;
    String restOfLineGetter = null;
    int maxOtherOffset = Integer.MIN_VALUE;

    for (FieldDescriptor desc : descriptors) {
      if (desc.fieldAnnotation.length() == Field.REST_OF_LINE) {
        if (restOfLineOffset != -1) {
          throw new FixedFormatException(format(
              "Only one @Field(length = -1) is allowed per record class %s, but found multiple",
              clazz.getName()));
        }
        restOfLineOffset = desc.fieldAnnotation.offset();
        restOfLineGetter = desc.target.getter.getDeclaringClass().getName() + "."
            + desc.target.getter.getName() + "()";
      } else {
        int effectiveEndOffset = desc.isRepeating
            ? desc.fieldAnnotation.offset() + desc.fieldAnnotation.count() * desc.fieldAnnotation.length() - 1
            : desc.fieldAnnotation.offset() + desc.fieldAnnotation.length() - 1;
        maxOtherOffset = Math.max(maxOtherOffset, effectiveEndOffset);
      }
    }

    if (restOfLineOffset == -1) return;

    if (maxOtherOffset >= restOfLineOffset) {
      throw new FixedFormatException(format(
          "@Field(length = -1) on %s must be the last field (highest offset) in the record,"
              + " but another field at offset %d comes after or at the same position",
          restOfLineGetter, maxOtherOffset));
    }
  }

  static void doValidateRestOfLineRecordLength(Class<?> clazz, List<FieldDescriptor> descriptors) {
    boolean hasRestOfLine = descriptors.stream()
        .anyMatch(desc -> desc.fieldAnnotation.length() == Field.REST_OF_LINE);
    if (!hasRestOfLine) return;
    Record record = clazz.getAnnotation(Record.class);
    if (record != null && record.length() != -1) {
      throw new FixedFormatException(format(
          "@Field(length = -1) is not compatible with @Record(length = %d) on %s "
              + "because record-level padding would corrupt the verbatim round-trip",
          record.length(), clazz.getName()));
    }
  }

  static void doValidateFieldNullChar(AnnotationTarget target, Field fieldAnnotation) {
    if (fieldAnnotation.nullChar() == Field.UNSET_NULL_CHAR) return;

    Class<?> typeToCheck;
    if (fieldAnnotation.count() > 1) {
      typeToCheck = new RepeatingFieldSupport().resolveElementType(target.getter);
    } else {
      FormatInstructionsBuilder instructionsBuilder = new FormatInstructionsBuilder();
      typeToCheck = instructionsBuilder.datatype(target.getter, fieldAnnotation);
    }

    if (typeToCheck.isPrimitive()) {
      throw new FixedFormatException(format(
          "@Field nullChar is not supported on primitive type %s on %s.%s()",
          typeToCheck.getName(),
          target.getter.getDeclaringClass().getName(),
          target.getter.getName()));
    }
  }

  static void doValidateNullValue(AnnotationTarget target, Field fieldAnnotation) {
    if (fieldAnnotation.nullValue().isEmpty()) return;
    if (fieldAnnotation.length() == Field.REST_OF_LINE) return;

    String getterRef = target.getter.getDeclaringClass().getName() + "." + target.getter.getName() + "()";

    if (fieldAnnotation.nullChar() != Field.UNSET_NULL_CHAR) {
      throw new FixedFormatException(format(
          "@Field nullValue \"%s\" and nullChar '%c' are mutually exclusive on %s",
          fieldAnnotation.nullValue(), fieldAnnotation.nullChar(), getterRef));
    }

    if (fieldAnnotation.nullValue().length() != fieldAnnotation.length()) {
      throw new FixedFormatException(format(
          "@Field nullValue \"%s\" has length %d but the field length is %d on %s",
          fieldAnnotation.nullValue(), fieldAnnotation.nullValue().length(), fieldAnnotation.length(), getterRef));
    }

    Class<?> typeToCheck;
    if (fieldAnnotation.count() > 1) {
      typeToCheck = new RepeatingFieldSupport().resolveElementType(target.getter);
    } else {
      FormatInstructionsBuilder instructionsBuilder = new FormatInstructionsBuilder();
      typeToCheck = instructionsBuilder.datatype(target.getter, fieldAnnotation);
    }

    if (typeToCheck.isPrimitive()) {
      throw new FixedFormatException(format(
          "@Field nullValue is not supported on primitive type %s on %s",
          typeToCheck.getName(), getterRef));
    }
  }

  static void doValidateFieldPattern(AnnotationTarget target, Field fieldAnnotation) {
    FormatInstructionsBuilder instructionsBuilder = new FormatInstructionsBuilder();
    Class<?> datatype = instructionsBuilder.datatype(target.getter, fieldAnnotation);
    FixedFormatPattern patternAnnotation = target.annotationSource.getAnnotation(FixedFormatPattern.class);
    String pattern;
    if (patternAnnotation != null) {
      pattern = patternAnnotation.value();
    } else if (java.time.LocalDate.class.equals(datatype)) {
      pattern = FixedFormatPatternData.LOCALDATE_DEFAULT.getPattern();
    } else if (java.time.LocalDateTime.class.equals(datatype)) {
      pattern = FixedFormatPatternData.DATETIME_DEFAULT.getPattern();
    } else {
      pattern = FixedFormatPatternData.DEFAULT.getPattern();
    }
    PatternValidator.validate(datatype, pattern);
  }
}
