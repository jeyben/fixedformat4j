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
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
   * JVM-level cache of record classes whose enum-field lengths have already been validated.
   * Validation is performed at most once per class (on the first {@code load} or {@code export}
   * call) and then skipped for subsequent calls.
   * <p>
   * <strong>Note:</strong> this set is never cleared. In multi-classloader environments
   * (e.g. application servers with hot-reload, OSGi containers) old {@link Class} references
   * may be retained here after their classloader is discarded, preventing garbage collection.
   * In such environments consider using a {@link java.lang.ref.WeakReference}-based map instead.
   * </p>
   */
  private static final Set<Class<?>> VALIDATED_CLASSES = Collections.newSetFromMap(new ConcurrentHashMap<>());

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
        throw new FixedFormatException(
            format("cannot export null value for nested @Record field %s.%s()",
                fixedFormatRecord.getClass().getName(), desc.target.getter.getName()));
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
    if (VALIDATED_CLASSES.contains(recordClass)) {
      return;
    }
    for (FieldDescriptor desc : ClassMetadataCache.INSTANCE.get(recordClass)) {
      validateFieldPattern(desc.target, desc.fieldAnnotation);
      validateEnumFieldLength(desc.target, desc.fieldAnnotation);
    }
    VALIDATED_CLASSES.add(recordClass);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void validateEnumFieldLength(AnnotationTarget target, Field fieldAnnotation) {
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

  private void validateFieldPattern(AnnotationTarget target, Field fieldAnnotation) {
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
    FormatInstructions formatdata = instructionsBuilder.build(annotationSource, fieldAnno, datatype);

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
