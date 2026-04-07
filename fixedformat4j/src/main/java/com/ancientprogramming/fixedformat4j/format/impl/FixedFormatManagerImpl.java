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
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.fetchData;
import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.getFixedFormatterInstance;
import static java.lang.String.format;

/**
 * Load and export objects to and from fixed formatted string representation
 *
 * @author Jacob von Eyben - https://eybenconsult.com
 * @since 1.0.0
 */
public class FixedFormatManagerImpl implements FixedFormatManager {

  private static final Logger LOG = LoggerFactory.getLogger(FixedFormatManagerImpl.class);

  /**
   * Pairs a getter {@link Method} (used for invocation and type resolution) with an
   * {@link AnnotatedElement} (used for supplementary annotation lookup). When the
   * {@link Field} annotation is on the getter, both references point to the same object.
   * When it is on a Java field, they differ.
   */
  private static final class AnnotationTarget {
    final Method getter;
    final AnnotatedElement annotationSource;

    private AnnotationTarget(Method getter, AnnotatedElement annotationSource) {
      this.getter = getter;
      this.annotationSource = annotationSource;
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

  /**
   * Collects all {@link AnnotationTarget}s for the given class.
   *
   * Pass 1 walks public methods; pass 2 walks declared fields (including superclasses).
   * Field annotations take priority over method annotations for the same property.
   * A conflict (both annotated) is logged as an error.
   */
  private List<AnnotationTarget> collectAnnotationTargets(Class<?> clazz) {
    LinkedHashMap<String, AnnotationTarget> targets = new LinkedHashMap<>();

    // Pass 1: method annotations
    for (Method method : clazz.getMethods()) {
      if (method.getAnnotation(Field.class) != null || method.getAnnotation(Fields.class) != null) {
        targets.put(stripMethodPrefix(method.getName()), AnnotationTarget.ofMethod(method));
      }
    }

    // Pass 2: field annotations — walk class hierarchy
    Class<?> current = clazz;
    while (current != null && current != Object.class) {
      for (java.lang.reflect.Field javaField : current.getDeclaredFields()) {
        if (javaField.getAnnotation(Field.class) == null && javaField.getAnnotation(Fields.class) == null) {
          continue;
        }
        Method getter = findGetter(clazz, javaField);
        String key = stripMethodPrefix(getter.getName());
        if (targets.containsKey(key)) {
          LOG.error("Configuration mismatch: @Field annotation found on both field '{}' and its getter method '{}' in class '{}'. The field annotation will be used.",
              javaField.getName(), getter.getName(), clazz.getName());
        }
        targets.put(key, AnnotationTarget.ofField(getter, javaField));
      }
      current = current.getSuperclass();
    }

    return new ArrayList<>(targets.values());
  }

  /**
   * @inheritDoc
   */
  public <T> T load(Class<T> fixedFormatRecordClass, String data) {
    HashMap<String, Object> foundData = new HashMap<String, Object>();
    HashMap<String, Class<?>> methodClass = new HashMap<String, Class<?>>();
    getAndAssertRecordAnnotation(fixedFormatRecordClass);

    T instance = createRecordInstance(fixedFormatRecordClass);

    for (AnnotationTarget target : collectAnnotationTargets(fixedFormatRecordClass)) {
      String methodName = stripMethodPrefix(target.getter.getName());
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
        Class datatype = methodClass.get(key);
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
   * @inheritDoc
   */
  public <T> String export(String template, T fixedFormatRecord) {
    StringBuffer result = new StringBuffer(template);
    Record record = getAndAssertRecordAnnotation(fixedFormatRecord.getClass());

    HashMap<Integer, String> foundData = new HashMap<Integer, String>();
    for (AnnotationTarget target : collectAnnotationTargets(fixedFormatRecord.getClass())) {
      Field fieldAnnotation = target.annotationSource.getAnnotation(Field.class);
      Fields fieldsAnnotation = target.annotationSource.getAnnotation(Fields.class);
      if (fieldAnnotation != null) {
        foundData.put(fieldAnnotation.offset(), exportDataAccordingFieldAnnotation(fixedFormatRecord, target, fieldAnnotation));
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
   * @inheritDoc
   */
  public <T> String export(T fixedFormatRecord) {
    return export("", fixedFormatRecord);
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
    Class datatype = getDatatype(getter, fieldAnno);

    FormatContext context = getFormatContext(datatype, fieldAnno);
    FixedFormatter formatter = getFixedFormatterInstance(context.getFormatter(), context);
    FormatInstructions formatdata = getFormatInstructions(annotationSource, fieldAnno);

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
      LOG.debug("the loaded data[" + loadedData + "]");
    }
    return loadedData;
  }

  private Class getDatatype(Method method, Field fieldAnno) {
    if (followsBeanStandard(method)) {
      return method.getReturnType();
    }
    throw new FixedFormatException(format("Cannot annotate method %s, with %s annotation. %s annotations must be placed on methods starting with 'get' or 'is'", method.getName(), fieldAnno.getClass().getName(), fieldAnno.getClass().getName()));
  }

  @SuppressWarnings({"unchecked"})
  private <T> String exportDataAccordingFieldAnnotation(T fixedFormatRecord, AnnotationTarget target, Field fieldAnno) {
    Class datatype = getDatatype(target.getter, fieldAnno);

    FormatContext<T> context = getFormatContext(datatype, fieldAnno);
    FixedFormatter formatter = getFixedFormatterInstance(context.getFormatter(), context);
    FormatInstructions formatdata = getFormatInstructions(target.annotationSource, fieldAnno);
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
      result = formatter.format(valueObject, formatdata);
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(format("exported %s ", result));
    }
    return result;
  }

  private Method findGetter(Class<?> clazz, java.lang.reflect.Field field) {
    String name = field.getName();
    String cap = Character.toUpperCase(name.charAt(0)) + name.substring(1);
    try {
      return clazz.getMethod("get" + cap);
    } catch (NoSuchMethodException e) {
      try {
        return clazz.getMethod("is" + cap);
      } catch (NoSuchMethodException e2) {
        throw new FixedFormatException(format("No getter found for field '%s' in class %s. Expected 'get%s()' or 'is%s()'.", name, clazz.getName(), cap, cap));
      }
    }
  }

  private String stripMethodPrefix(String name) {
    if (name.startsWith("get") || name.startsWith("set")) {
      return name.substring(3);
    } else if (name.startsWith("is")) {
      return name.substring(2);
    } else {
      return name;
    }
  }

  @SuppressWarnings({"unchecked"})
  private <T> FormatContext<T> getFormatContext(Class<T> datatype, Field fieldAnno) {
    if (fieldAnno != null) {
      return new FormatContext(fieldAnno.offset(), datatype, fieldAnno.formatter());
    }
    return null;
  }

  private FormatInstructions getFormatInstructions(AnnotatedElement annotationSource, Field fieldAnno) {
    FixedFormatPatternData patternData = getFixedFormatPatternData(annotationSource.getAnnotation(FixedFormatPattern.class));
    FixedFormatBooleanData booleanData = getFixedFormatBooleanData(annotationSource.getAnnotation(FixedFormatBoolean.class));
    FixedFormatNumberData numberData = getFixedFormatNumberData(annotationSource.getAnnotation(FixedFormatNumber.class));
    FixedFormatDecimalData decimalData = getFixedFormatDecimalData(annotationSource.getAnnotation(FixedFormatDecimal.class));
    return new FormatInstructions(fieldAnno.length(), fieldAnno.align(), fieldAnno.paddingChar(), patternData, booleanData, numberData, decimalData);
  }

  private FixedFormatPatternData getFixedFormatPatternData(FixedFormatPattern annotation) {
    if (annotation != null) {
      return new FixedFormatPatternData(annotation.value());
    }
    return FixedFormatPatternData.DEFAULT;
  }

  private FixedFormatBooleanData getFixedFormatBooleanData(FixedFormatBoolean annotation) {
    if (annotation != null) {
      return new FixedFormatBooleanData(annotation.trueValue(), annotation.falseValue());
    }
    return FixedFormatBooleanData.DEFAULT;
  }

  private FixedFormatNumberData getFixedFormatNumberData(FixedFormatNumber annotation) {
    if (annotation != null) {
      return new FixedFormatNumberData(annotation.sign(), annotation.positiveSign(), annotation.negativeSign());
    }
    return FixedFormatNumberData.DEFAULT;
  }

  private FixedFormatDecimalData getFixedFormatDecimalData(FixedFormatDecimal annotation) {
    if (annotation != null) {
      return new FixedFormatDecimalData(annotation.decimals(), annotation.useDecimalDelimiter(), annotation.decimalDelimiter(), RoundingMode.valueOf(annotation.roundingMode()));
    }
    return FixedFormatDecimalData.DEFAULT;
  }

  private boolean followsBeanStandard(Method method) {
    String methodName = method.getName();
    return methodName.startsWith("get") || methodName.startsWith("is");
  }

  private <T> T createRecordInstance(Class<T> fixedFormatRecordClass) {
    T instance;
    try {
      Constructor<T> constructor = fixedFormatRecordClass.getDeclaredConstructor();
      instance = constructor.newInstance();
    } catch (NoSuchMethodException e) {
      Class declaringClass = fixedFormatRecordClass.getDeclaringClass();
      if (declaringClass != null) {
        try {
          Object declaringClassInstance;
          try {
            Constructor declaringClassConstructor = declaringClass.getDeclaredConstructor();
            declaringClassInstance = declaringClassConstructor.newInstance();
          } catch (NoSuchMethodException dex) {
            throw new FixedFormatException(format("Trying to create instance of innerclass %s, but the declaring class %s is missing a default constructor which is nessesary to be loaded through %s", fixedFormatRecordClass.getName(), declaringClass.getName(), getClass().getName()));
          } catch (Exception de) {
            throw new FixedFormatException(format("unable to create instance of declaring class %s, which is needed to instansiate %s", declaringClass.getName(), fixedFormatRecordClass.getName()), e);
          }
          Constructor<T> constructor = fixedFormatRecordClass.getDeclaredConstructor(declaringClass);
          instance = constructor.newInstance(declaringClassInstance);
        } catch (FixedFormatException ex) {
          throw ex;
        } catch (NoSuchMethodException ex) {
          throw new FixedFormatException(format("%s is missing a default constructor which is nessesary to be loaded through %s", fixedFormatRecordClass.getName(), getClass().getName()));
        } catch (Exception ex) {
          throw new FixedFormatException(format("unable to create instance of %s", fixedFormatRecordClass.getName()), e);
        }
      } else {
        throw new FixedFormatException(format("%s is missing a default constructor which is nessesary to be loaded through %s", fixedFormatRecordClass.getName(), getClass().getName()));
      }
    } catch (Exception e) {
      throw new FixedFormatException(format("unable to create instance of %s", fixedFormatRecordClass.getName()), e);
    }
    return instance;
  }
}
