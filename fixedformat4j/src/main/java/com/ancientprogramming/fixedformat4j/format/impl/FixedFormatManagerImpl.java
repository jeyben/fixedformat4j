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

import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
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
        FieldValidator.doValidateFieldPattern(desc.target, desc.fieldAnnotation);
        FieldValidator.doValidateEnumFieldLength(desc.target, desc.fieldAnnotation);
        FieldValidator.doValidateFieldNullChar(desc.target, desc.fieldAnnotation);
        FieldValidator.doValidateRestOfLineField(desc.target, desc.fieldAnnotation);
      }
      FieldValidator.doValidateRestOfLineIsLastField(clazz, descriptors);
      FieldValidator.doValidateRestOfLineRecordLength(clazz, descriptors);
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
    StringBuilder result = new StringBuilder(template);
    Record record = getAndAssertRecordAnnotation(fixedFormatRecord.getClass());
    validatePatterns(fixedFormatRecord.getClass());

    List<FieldDescriptor> descriptors = ClassMetadataCache.INSTANCE.get(fixedFormatRecord.getClass());
    HashMap<Integer, String> foundData = new HashMap<>(descriptors.size() * 2);

    for (FieldDescriptor desc : descriptors) {
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
        formatted = StringUtils.repeat(desc.fieldAnnotation.paddingChar(), desc.fieldAnnotation.length());
      } else if (valueObject == null && NullCharSupport.isNullCharActive(desc.formatInstructions)) {
        formatted = StringUtils.repeat(desc.formatInstructions.getNullChar(), desc.formatInstructions.getLength());
      } else {
        formatted = ((FixedFormatter<Object>) desc.formatter).format(valueObject, desc.formatInstructions);
      }

      if (LOG.isDebugEnabled()) {
        LOG.debug(format("exported %s ", formatted));
      }
      foundData.put(desc.fieldAnnotation.offset(), formatted);
    }

    for (Map.Entry<Integer, String> entry : foundData.entrySet()) {
      appendData(result, record.paddingChar(), entry.getKey(), entry.getValue());
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

  private static void appendData(StringBuilder result, Character paddingChar, Integer offset, String data) {
    int zeroBasedOffset = offset - 1;
    while (result.length() < zeroBasedOffset) {
      result.append(paddingChar);
    }
    int length = data.length();
    if (result.length() < zeroBasedOffset + length) {
      int needed = (zeroBasedOffset + length) - result.length();
      for (int i = 0; i < needed; i++) {
        result.append(paddingChar);
      }
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
}
