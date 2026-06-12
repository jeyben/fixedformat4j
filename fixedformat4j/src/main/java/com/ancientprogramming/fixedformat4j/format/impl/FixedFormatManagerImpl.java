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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

  public FixedFormatManagerImpl() {
    this.metadataCache = ClassMetadataCache.INSTANCE;
  }

  private FixedFormatManagerImpl(Map<Class<?>, Class<? extends FixedFormatter<?>>> customRegistry) {
    this.metadataCache = customRegistry.isEmpty()
        ? ClassMetadataCache.INSTANCE
        : new ClassMetadataCache(customRegistry);
  }

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
   * Returns a builder for constructing a {@code FixedFormatManagerImpl} with a custom type registry.
   *
   * @return a new {@code Builder}; never {@code null}
   * @since 1.9.0
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for {@link FixedFormatManagerImpl} that allows registering custom type-to-formatter
   * mappings. Custom registrations shadow built-in formatters; last registration wins on duplicates.
   *
   * @since 1.9.0
   */
  public static final class Builder {
    private final Map<Class<?>, Class<? extends FixedFormatter<?>>> registry = new LinkedHashMap<>();

    private Builder() {}

    /**
     * Registers a formatter class for the given type. If the type already has a mapping
     * (either a prior {@code registerType} call or a built-in), this registration overwrites it.
     * Last registration wins — no exception is thrown on duplicates.
     *
     * @param type           the Java type to map; must not be {@code null}
     * @param formatterClass the formatter to use for {@code type}; must not be {@code null}
     * @return this builder, for chaining
     */
    public <T> Builder registerType(Class<T> type, Class<? extends FixedFormatter<T>> formatterClass) {
      Objects.requireNonNull(type, "type must not be null");
      Objects.requireNonNull(formatterClass, "formatterClass must not be null");
      registry.put(type, formatterClass);
      return this;
    }

    /**
     * Builds and returns a {@link FixedFormatManager} with the registered type mappings.
     *
     * @return a new manager instance; never {@code null}
     */
    public FixedFormatManager build() {
      return new FixedFormatManagerImpl(Collections.unmodifiableMap(new LinkedHashMap<>(registry)));
    }
  }

  private final ClassValue<Boolean> validatedClasses = new ClassValue<>() {
    @Override
    protected Boolean computeValue(Class<?> clazz) {
      List<FieldDescriptor> descriptors = metadataCache.get(clazz);
      for (FieldDescriptor desc : descriptors) {
        FieldValidator.doValidateFieldPattern(desc.target, desc.fieldAnnotation);
        FieldValidator.doValidateEnumFieldLength(desc.target, desc.fieldAnnotation);
        FieldValidator.doValidateFieldNullChar(desc.target, desc.fieldAnnotation);
        FieldValidator.doValidateNullValue(desc.target, desc.fieldAnnotation);
        FieldValidator.doValidateRestOfLineField(desc.target, desc.fieldAnnotation);
      }
      FieldValidator.doValidateRestOfLineIsLastField(clazz, descriptors);
      FieldValidator.doValidateRestOfLineRecordLength(clazz, descriptors);
      return Boolean.TRUE;
    }
  };

  private final ClassMetadataCache metadataCache;
  private final RecordInstantiator recordInstantiator = new RecordInstantiator();
  private final RepeatingFieldSupport repeatingFieldSupport = new RepeatingFieldSupport();

  /**
   * {@inheritDoc}
   */
  public <T> T load(Class<T> fixedFormatRecordClass, String data) {
    getAndAssertRecordAnnotation(fixedFormatRecordClass);
    validatePatterns(fixedFormatRecordClass);

    T instance = recordInstantiator.instantiate(fixedFormatRecordClass);

    for (FieldDescriptor desc : metadataCache.get(fixedFormatRecordClass)) {
      if (!desc.isLoadField) continue;

      Object value;
      if (desc.isRepeating) {
        value = repeatingFieldSupport.read(fixedFormatRecordClass, data, desc);
      } else {
        String dataToParse = fetchData(data, desc.formatInstructions, desc.context);
        if (desc.isNestedRecord) {
          value = load(desc.datatype, dataToParse);
        } else if (NullSupport.isNullSliceOrValue(dataToParse, desc.formatInstructions)) {
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

    List<FieldDescriptor> descriptors = metadataCache.get(fixedFormatRecord.getClass());
    HashMap<Integer, String> foundData = new HashMap<>(descriptors.size() * 2);

    for (FieldDescriptor desc : descriptors) {
      if (desc.isRepeating) {
        repeatingFieldSupport.export(fixedFormatRecord, desc, foundData);
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
        formatted = String.valueOf(desc.fieldAnnotation.paddingChar()).repeat(desc.fieldAnnotation.length());
      } else if (valueObject == null && NullSupport.isNullCharActive(desc.formatInstructions)) {
        formatted = String.valueOf(desc.formatInstructions.getNullChar()).repeat(desc.formatInstructions.getLength());
      } else if (valueObject == null && NullSupport.isNullValueActive(desc.formatInstructions)) {
        formatted = desc.formatInstructions.getNullValue();
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
    validatedClasses.get(recordClass);
  }

  private static void appendData(StringBuilder result, char paddingChar, int offset, String data) {
    int zeroBasedOffset = offset - 1;
    int end = zeroBasedOffset + data.length();
    while (result.length() < end) {
      result.append(paddingChar);
    }
    result.replace(zeroBasedOffset, end, data);
  }

  private <T> Record getAndAssertRecordAnnotation(Class<T> fixedFormatRecordClass) {
    Record recordAnno = fixedFormatRecordClass.getAnnotation(Record.class);
    if (recordAnno == null) {
      throw new FixedFormatException(format("%s has to be marked with the record annotation to be loaded", fixedFormatRecordClass.getName()));
    }
    return recordAnno;
  }
}
