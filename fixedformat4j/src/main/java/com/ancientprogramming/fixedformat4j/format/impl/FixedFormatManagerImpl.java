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
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.fetchData;
import static java.lang.String.format;

/**
 * Load and export objects to and from fixed formatted string representation
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class FixedFormatManagerImpl implements FixedFormatManager {

  private static final Logger LOG = LoggerFactory.getLogger(FixedFormatManagerImpl.class);

  /**
   * Returns a new instance of this implementation as a {@link FixedFormatManager}.
   *
   * @return a new {@code FixedFormatManagerImpl}; never {@code null}
   * @since 1.8.0
   */
  public static FixedFormatManager create() {
    return new FixedFormatManagerImpl();
  }

  /**
   * Tracks which record classes have already been validated. The sentinel value is stored inside
   * each {@link Class} object via {@link ClassValue}, so it is automatically GC'd when the
   * defining classloader becomes unreachable — preventing classloader leaks in hot-reload and
   * multi-classloader environments. {@link ClassValue#computeValue} is invoked at most once per
   * class, ensuring validation runs exactly once per class per JVM lifetime.
   */
  private static final ClassValue<Boolean> VALIDATED_CLASSES = new ClassValue<Boolean>() {
    @Override
    protected Boolean computeValue(Class<?> clazz) {
      List<FieldDescriptor> descriptors = ClassMetadataCache.INSTANCE.get(clazz);
      for (FieldDescriptor desc : descriptors) {
        doValidateFieldPattern(desc.target, desc.fieldAnnotation);
        doValidateEnumFieldLength(desc.target, desc.fieldAnnotation);
        doValidateFieldNullChar(desc.target, desc.fieldAnnotation);
        doValidateRestOfLineField(desc.target, desc.fieldAnnotation);
      }
      doValidateRestOfLineIsLastField(clazz, descriptors);
      doValidateRestOfLineRecordLength(clazz, descriptors);
      return Boolean.TRUE;
    }
  };

  private final RecordInstantiator recordInstantiator = new RecordInstantiator();
  private final RepeatingFieldSupport repeatingFieldSupport = new RepeatingFieldSupport();

  /**
   * {@inheritDoc}
   */
  public <T> T load(Class<T> fixedFormatRecordClass, String data) {
    getAndAssertRecordAnnotation(fixedFormatRecordClass);
    validatePatterns(fixedFormatRecordClass);

    T instance = recordInstantiator.instantiate(fixedFormatRecordClass);

    for (FieldDescriptor desc : ClassMetadataCache.INSTANCE.get(fixedFormatRecordClass)) {
      if (!desc.isLoadField) continue;

      Object value;
      if (desc.isRepeating) {
        value = repeatingFieldSupport.read(fixedFormatRecordClass, data, desc.target.getter, desc.target.annotationSource, desc.fieldAnnotation);
      } else {
        String dataToParse = fetchData(data, desc.formatInstructions, desc.context);
        if (desc.isNestedRecord) {
          value = load(desc.datatype, dataToParse);
        } else if (NullCharSupport.isNullSlice(dataToParse, desc.formatInstructions)) {
          value = null;
        } else {
          try {
            value = desc.formatter.parse(dataToParse, desc.formatInstructions);
          } catch (RuntimeException e) {
            throw new ParseException(data, dataToParse, fixedFormatRecordClass, desc.target.getter, desc.context, desc.formatInstructions, e);
          }
        }
      }

      if (value != null && desc.setterHandle != null) {
        try {
          desc.setterHandle.invoke(instance, value);
        } catch (Throwable e) {
          throw new FixedFormatException(
              format("could not invoke method %s.%s(%s)", fixedFormatRecordClass.getName(), desc.setter.getName(), desc.datatype), e);
        }
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug("the loaded data[{}]", value);
      }
    }

    return instance;
  }

  /**
   * {@inheritDoc}
   */
  public <T> String export(String template, T fixedFormatRecord) {
    StringBuffer result = new StringBuffer(template);
    Record record = getAndAssertRecordAnnotation(fixedFormatRecord.getClass());
    validatePatterns(fixedFormatRecord.getClass());

    HashMap<Integer, String> foundData = new HashMap<Integer, String>();

    for (FieldDescriptor desc : ClassMetadataCache.INSTANCE.get(fixedFormatRecord.getClass())) {
      if (desc.isRepeating) {
        repeatingFieldSupport.export(fixedFormatRecord, desc.target, desc.fieldAnnotation, foundData);
        continue;
      }

      Object valueObject;
      try {
        valueObject = desc.target.getterHandle.invoke(fixedFormatRecord);
      } catch (Throwable e) {
        throw new FixedFormatException(
            format("could not invoke method %s.%s(%s)", fixedFormatRecord.getClass().getName(), desc.target.getter.getName(), desc.datatype), e);
      }

      String formatted;
      if (valueObject != null && valueObject.getClass().getAnnotation(Record.class) != null) {
        formatted = export(valueObject);
      } else if (desc.isNestedRecord) {
        formatted = StringUtils.repeat(String.valueOf(desc.fieldAnnotation.paddingChar()), desc.fieldAnnotation.length());
      } else if (valueObject == null && NullCharSupport.isNullCharActive(desc.formatInstructions)) {
        formatted = StringUtils.repeat(String.valueOf(desc.formatInstructions.getNullChar()), desc.formatInstructions.getLength());
      } else {
        formatted = ((FixedFormatter<Object>) desc.formatter).format(valueObject, desc.formatInstructions);
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug(format("exported %s ", formatted));
      }
      foundData.put(desc.fieldAnnotation.offset(), formatted);
    }

    for (Integer offset : foundData.keySet()) {
      appendData(result, record.paddingChar(), offset, foundData.get(offset));
    }

    if (record.length() != -1) {
      while (result.length() < record.length()) {
        result.append(record.paddingChar());
      }
    }
    return result.toString();
  }

  /**
   * {@inheritDoc}
   */
  public <T> String export(T fixedFormatRecord) {
    return export("", fixedFormatRecord);
  }

  private void validatePatterns(Class<?> recordClass) {
    VALIDATED_CLASSES.get(recordClass);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private static void doValidateEnumFieldLength(AnnotationTarget target, Field fieldAnnotation) {
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

  private static void doValidateRestOfLineField(AnnotationTarget target, Field fieldAnnotation) {
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
  }

  private static void doValidateRestOfLineIsLastField(Class<?> clazz, List<FieldDescriptor> descriptors) {
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

  private static void doValidateRestOfLineRecordLength(Class<?> clazz, List<FieldDescriptor> descriptors) {
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

  private static void doValidateFieldNullChar(AnnotationTarget target, Field fieldAnnotation) {
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

  private static void doValidateFieldPattern(AnnotationTarget target, Field fieldAnnotation) {
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

  private void appendData(StringBuffer result, Character paddingChar, Integer offset, String data) {
    int zeroBasedOffset = offset - 1;
    while (result.length() < zeroBasedOffset) {
      result.append(paddingChar);
    }
    int length = data.length();
    if (result.length() < zeroBasedOffset + length) {
      result.append(StringUtils.leftPad("", (zeroBasedOffset + length) - result.length(), paddingChar));
    }
    result.replace(zeroBasedOffset, zeroBasedOffset + length, data);
  }

  private <T> Record getAndAssertRecordAnnotation(Class<T> fixedFormatRecordClass) {
    Record recordAnno = fixedFormatRecordClass.getAnnotation(Record.class);
    if (recordAnno == null) {
      throw new FixedFormatException(format("%s has to be marked with the record annotation to be loaded", fixedFormatRecordClass.getName()));
    }
    return recordAnno;
  }

  /**
   * Reads a single non-repeating field from {@code data} and returns the parsed value.
   * Protected for backward-compatibility with subclasses; the main load path uses the
   * {@link ClassMetadataCache} directly.
   *
   * @deprecated Internal use only. Will be made private in a future release.
   */
  @Deprecated
  @SuppressWarnings({"unchecked"})
  protected <T> Object readDataAccordingFieldAnnotation(Class<T> clazz, String data, Method getter, java.lang.reflect.AnnotatedElement annotationSource, Field fieldAnno) throws ParseException {
    repeatingFieldSupport.validateCount(getter, fieldAnno);

    if (fieldAnno.count() > 1) {
      return repeatingFieldSupport.read(clazz, data, getter, annotationSource, fieldAnno);
    }

    FormatInstructionsBuilder instructionsBuilder = new FormatInstructionsBuilder();
    Class<?> datatype = instructionsBuilder.datatype(getter, fieldAnno);
    FormatContext<?> context = instructionsBuilder.context(datatype, fieldAnno);
    FixedFormatter<?> formatter = com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.getFixedFormatterInstance(context.getFormatter(), context);
    FormatInstructions formatdata = instructionsBuilder.build(annotationSource, fieldAnno, datatype, getter.getDeclaringClass());

    String dataToParse = fetchData(data, formatdata, context);

    java.lang.annotation.Annotation recordAnno = datatype.getAnnotation(Record.class);
    if (recordAnno != null) {
      return load(datatype, dataToParse);
    }
    try {
      return formatter.parse(dataToParse, formatdata);
    } catch (RuntimeException e) {
      throw new ParseException(data, dataToParse, clazz, getter, context, formatdata, e);
    }
  }
}
