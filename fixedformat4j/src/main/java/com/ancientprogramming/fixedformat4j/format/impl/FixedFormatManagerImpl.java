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

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Fields;
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.fetchData;
import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.getFixedFormatterInstance;
import static java.lang.String.format;

/**
 * Load and export objects to and from fixed formatted string representation
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class FixedFormatManagerImpl implements FixedFormatManager {

  private static final Logger LOG = LoggerFactory.getLogger(FixedFormatManagerImpl.class);
  private static final Set<Class<?>> VALIDATED_CLASSES = Collections.newSetFromMap(new ConcurrentHashMap<>());

  private final AnnotationScanner annotationScanner = new AnnotationScanner();
  private final FormatInstructionsBuilder instructionsBuilder = new FormatInstructionsBuilder();
  private final RecordInstantiator recordInstantiator = new RecordInstantiator();
  private final RepeatingFieldSupport repeatingFieldSupport = new RepeatingFieldSupport();

  /**
   * {@inheritDoc}
   */
  public <T> T load(Class<T> fixedFormatRecordClass, String data) {
    HashMap<String, Object> foundData = new HashMap<String, Object>();
    HashMap<String, Class<?>> methodClass = new HashMap<String, Class<?>>();
    getAndAssertRecordAnnotation(fixedFormatRecordClass);
    validatePatterns(fixedFormatRecordClass);

    T instance = recordInstantiator.instantiate(fixedFormatRecordClass);

    for (AnnotationTarget target : annotationScanner.scan(fixedFormatRecordClass)) {
      String methodName = annotationScanner.stripMethodPrefix(target.getter.getName());
      Field fieldAnnotation = target.annotationSource.getAnnotation(Field.class);
      Fields fieldsAnnotation = target.annotationSource.getAnnotation(Fields.class);
      if (fieldAnnotation != null) {
        readFieldData(fixedFormatRecordClass, data, foundData, methodClass, target, methodName, fieldAnnotation);
      } else if (fieldsAnnotation != null) {
        if (fieldsAnnotation.value() == null || fieldsAnnotation.value().length == 0) {
          throw new FixedFormatException(format("%s annotation must contain minimum one %s annotation", Fields.class.getName(), Field.class.getName()));
        }
        readFieldData(fixedFormatRecordClass, data, foundData, methodClass, target, methodName, fieldsAnnotation.value()[0]);
      }
    }

    Set<String> keys = foundData.keySet();
    for (String key : keys) {
      String setterMethodName = "set" + key;
      Object foundDataObj = foundData.get(key);
      if (foundDataObj != null) {
        Class<?> datatype = methodClass.get(key);
        Method method;
        try {
          method = fixedFormatRecordClass.getMethod(setterMethodName, datatype);
        } catch (NoSuchMethodException e) {
          throw new FixedFormatException(format("setter method named %s.%s(%s) does not exist", fixedFormatRecordClass.getName(), setterMethodName, datatype));
        }
        try {
          method.invoke(instance, foundData.get(key));
        } catch (Exception e) {
          throw new FixedFormatException(format("could not invoke method %s.%s(%s)", fixedFormatRecordClass.getName(), setterMethodName, datatype), e);
        }
      }
    }
    return instance;
  }

  private <T> void readFieldData(Class<T> fixedFormatRecordClass, String data, HashMap<String, Object> foundData, HashMap<String, Class<?>> methodClass, AnnotationTarget target, String methodName, Field fieldAnnotation) {
    Object loadedData = readDataAccordingFieldAnnotation(fixedFormatRecordClass, data, target.getter, target.annotationSource, fieldAnnotation);
    foundData.put(methodName, loadedData);
    methodClass.put(methodName, target.getter.getReturnType());
  }

  /**
   * {@inheritDoc}
   */
  public <T> String export(String template, T fixedFormatRecord) {
    StringBuffer result = new StringBuffer(template);
    Record record = getAndAssertRecordAnnotation(fixedFormatRecord.getClass());
    validatePatterns(fixedFormatRecord.getClass());

    HashMap<Integer, String> foundData = new HashMap<Integer, String>();
    for (AnnotationTarget target : annotationScanner.scan(fixedFormatRecord.getClass())) {
      Field fieldAnnotation = target.annotationSource.getAnnotation(Field.class);
      Fields fieldsAnnotation = target.annotationSource.getAnnotation(Fields.class);
      if (fieldAnnotation != null) {
        if (fieldAnnotation.count() > 1) {
          repeatingFieldSupport.export(fixedFormatRecord, target, fieldAnnotation, foundData);
        } else {
          foundData.put(fieldAnnotation.offset(), exportDataAccordingFieldAnnotation(fixedFormatRecord, target, fieldAnnotation));
        }
      } else if (fieldsAnnotation != null) {
        for (Field field : fieldsAnnotation.value()) {
          foundData.put(field.offset(), exportDataAccordingFieldAnnotation(fixedFormatRecord, target, field));
        }
      }
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
    for (AnnotationTarget target : annotationScanner.scan(recordClass)) {
      Field fieldAnnotation = target.annotationSource.getAnnotation(Field.class);
      Fields fieldsAnnotation = target.annotationSource.getAnnotation(Fields.class);
      if (fieldAnnotation != null) {
        validateFieldPattern(target, fieldAnnotation);
      } else if (fieldsAnnotation != null) {
        for (Field field : fieldsAnnotation.value()) {
          validateFieldPattern(target, field);
        }
      }
    }
    VALIDATED_CLASSES.add(recordClass);
  }

  private void validateFieldPattern(AnnotationTarget target, Field fieldAnnotation) {
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

  @SuppressWarnings({"unchecked"})
  protected <T> Object readDataAccordingFieldAnnotation(Class<T> clazz, String data, Method getter, AnnotatedElement annotationSource, Field fieldAnno) throws ParseException {
    repeatingFieldSupport.validateCount(getter, fieldAnno);

    if (fieldAnno.count() > 1) {
      return repeatingFieldSupport.read(clazz, data, getter, annotationSource, fieldAnno);
    }

    Class<?> datatype = instructionsBuilder.datatype(getter, fieldAnno);

    FormatContext<?> context = instructionsBuilder.context(datatype, fieldAnno);
    FixedFormatter<?> formatter = getFixedFormatterInstance(context.getFormatter(), context);
    FormatInstructions formatdata = instructionsBuilder.build(annotationSource, fieldAnno, datatype);

    String dataToParse = fetchData(data, formatdata, context);

    Object loadedData;

    Annotation recordAnno = datatype.getAnnotation(Record.class);
    if (recordAnno != null) {
      loadedData = load(datatype, dataToParse);
    } else {
      try {
        loadedData = formatter.parse(dataToParse, formatdata);
      } catch (RuntimeException e) {
        throw new ParseException(data, dataToParse, clazz, getter, context, formatdata, e);
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("the loaded data[{}]", loadedData);
    }
    return loadedData;
  }

  @SuppressWarnings({"unchecked"})
  private <T> String exportDataAccordingFieldAnnotation(T fixedFormatRecord, AnnotationTarget target, Field fieldAnno) {
    repeatingFieldSupport.validateCount(target.getter, fieldAnno);

    Class<?> datatype = instructionsBuilder.datatype(target.getter, fieldAnno);

    FormatContext<?> context = instructionsBuilder.context(datatype, fieldAnno);
    FixedFormatter<?> formatter = getFixedFormatterInstance(context.getFormatter(), context);
    FormatInstructions formatdata = instructionsBuilder.build(target.annotationSource, fieldAnno, datatype);
    Object valueObject;
    try {
      valueObject = target.getter.invoke(fixedFormatRecord);
    } catch (Exception e) {
      throw new FixedFormatException(format("could not invoke method %s.%s(%s)", fixedFormatRecord.getClass().getName(), target.getter.getName(), datatype), e);
    }

    String result;
    if (valueObject != null && valueObject.getClass().getAnnotation(Record.class) != null) {
      result = export(valueObject);
    } else {
      result = ((FixedFormatter<Object>) formatter).format(valueObject, formatdata);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(format("exported %s ", result));
    }
    return result;
  }
}
