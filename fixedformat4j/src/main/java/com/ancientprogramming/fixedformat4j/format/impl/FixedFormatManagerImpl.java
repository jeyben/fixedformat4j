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

import com.ancientprogramming.fixedformat4j.annotation.*;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.*;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Set;

import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.fetchData;
import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.getFixedFormatterInstance;
import static java.lang.String.format;

/**
 * Load and export objects to and from fixed formatted string representation
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class FixedFormatManagerImpl implements FixedFormatManager {

  private static final Log LOG = LogFactory.getLog(FixedFormatManagerImpl.class);

  /**
   * @inheritDoc
   */
  public <T> T load(Class<T> fixedFormatRecordClass, String data) {
    HashMap<String, Object> foundData = new HashMap<String, Object>();
    HashMap<String, Class<?>> methodClass = new HashMap<String, Class<?>>();
    //assert the record is marked with a Record
    getAndAssertRecordAnnotation(fixedFormatRecordClass);

    //create instance to set data into
    T instance = createRecordInstance(fixedFormatRecordClass);

    //look for setter annotations and read data from the 'data' string
    Method[] allMethods = fixedFormatRecordClass.getMethods();
    for (Method method : allMethods) {
      String methodName = stripMethodPrefix(method.getName());
      Field fieldAnnotation = method.getAnnotation(Field.class);
      Fields fieldsAnnotation = method.getAnnotation(Fields.class);

      if ((fieldAnnotation != null || fieldsAnnotation != null) && !followsBeanStandard(method)) {
        throw new FixedFormatException(format("Cannot annotate method %s, with %s annotation. %s annotations must be placed on methods starting with 'get' or 'is'", method.getName(), fixedFormatRecordClass.getName(), fixedFormatRecordClass.getName()));
      }

      if (fieldAnnotation != null) {
        readFieldData(fixedFormatRecordClass, data, foundData, methodClass, method, methodName, fieldAnnotation);
      } else if (fieldsAnnotation != null) {
        checkValid(fieldsAnnotation);
        if (isWrapped(fieldsAnnotation)) {
          readFieldsData(fixedFormatRecordClass, data, foundData, methodClass, method, methodName, fieldsAnnotation.value());
        } else {
          readFieldsData(fixedFormatRecordClass, data, foundData, methodClass, method, methodName, fieldsAnnotation);
        }
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

  private <T> void readFieldsData(Class<T> fixedFormatRecordClass, String data, HashMap<String, Object> foundData, HashMap<String, Class<?>> methodClass, Method method, String methodName, Field[] fieldAnnotations) {
    Class dataType = method.getReturnType();
    Object loadedData = Array.newInstance(dataType.getComponentType(), fieldAnnotations.length);

    for (int i = 0; i < fieldAnnotations.length; i++) {
      Array.set(loadedData, i, readDataAccordingFieldAnnotation(fixedFormatRecordClass, data, method, fieldAnnotations[i], dataType.getComponentType()));
    }

    foundData.put(methodName, loadedData);
    methodClass.put(methodName, dataType);
  }

  private <T> void readFieldsData(Class<T> fixedFormatRecordClass, String data, HashMap<String, Object> foundData, HashMap<String, Class<?>> methodClass, Method method, String methodName, Fields fieldsAnnotation) {
    Class dataType = method.getReturnType();
    Object loadedData = Array.newInstance(dataType.getComponentType(), fieldsAnnotation.count());

    for (int i = 0; i < fieldsAnnotation.count(); i++) {
      Array.set(loadedData, i, readDataAccordingFieldAnnotation(fixedFormatRecordClass, data, method, fieldsAnnotation, dataType.getComponentType(), i));
    }

    foundData.put(methodName, loadedData);
    methodClass.put(methodName, dataType);
  }

  private <T> void readFieldData(Class<T> fixedFormatRecordClass, String data, HashMap<String, Object> foundData, HashMap<String, Class<?>> methodClass, Method method, String methodName, Field fieldAnnotation) {
    Object loadedData = readDataAccordingFieldAnnotation(fixedFormatRecordClass, data, method, fieldAnnotation, method.getReturnType());
    foundData.put(methodName, loadedData);
    methodClass.put(methodName, method.getReturnType());
  }

  private <T> T createRecordInstance(Class<T> fixedFormatRecordClass) {
    T instance;
    try {
      Constructor<T> constructor = fixedFormatRecordClass.getDeclaredConstructor();
      instance = constructor.newInstance();
    } catch (NoSuchMethodException e) {
      //If the class is a possible inner class do some more work
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

  /**
   * @inheritDoc
   */
  public <T> String export(String template, T fixedFormatRecord) {
    StringBuffer result = new StringBuffer(template);
    Record record = getAndAssertRecordAnnotation(fixedFormatRecord.getClass());

    Class fixedFormatRecordClass = fixedFormatRecord.getClass();
    // hashmap containing offset and data to write
    HashMap<Integer, String> foundData = new HashMap<Integer, String>();
    Method[] allMethods = fixedFormatRecordClass.getMethods();
    for (Method method : allMethods) {
      Field fieldAnnotation = method.getAnnotation(Field.class);
      Fields fieldsAnnotation = method.getAnnotation(Fields.class);

      if (fieldAnnotation == null && fieldsAnnotation == null) {
        continue;
      }

      Object valueObject;
      try {
        valueObject = method.invoke(fixedFormatRecord);
      } catch (Exception e) {
        throw new FixedFormatException(format("could not invoke method %s.%s(%s)", fixedFormatRecord.getClass().getName(), method.getName(), fixedFormatRecord.getClass()), e);
      }

      FixedFormatter formatter;

      if (fieldAnnotation != null) {
        formatter = getFormatter(method, fieldAnnotation);
        FormatInstructions formatdata = getFormatInstructions(method, fieldAnnotation);
        String exportedData = exportDataAccordingFieldAnnotation(formatter, formatdata, valueObject);
        foundData.put(fieldAnnotation.offset(), exportedData);
      } else {
        if (valueObject == null) {
          throw new FixedFormatException(format("Expected an array but got null when exporting %s!", stripMethodPrefix(method.getName())));
        }

        FormatInstructions formatdata;

        Field[] fields = fieldsAnnotation.value();
        if (fieldsAnnotation.offset() < 0) {
          if (fields.length < 1) {
            throw new FixedFormatException(format("Expected at least one Field inside Fields for %s!", stripMethodPrefix(method.getName())));
          }

          // NOTE: All fields will use the same formatter!
          formatter = getFormatter(method, fields[0]);
          formatdata = getFormatInstructions(method, fields[0]);

          int len = Array.getLength(valueObject);
          if (len != fields.length) {
            throw new FixedFormatException(format("Length mismatch for %s, expected %d but got %d elements!", stripMethodPrefix(method.getName()), fields.length, len));
          }

          for (int i = 0; i < len; i++) {
            Object item = Array.get(valueObject, i);
            String exportedData = exportDataAccordingFieldAnnotation(formatter, formatdata, item);
            foundData.put(fields[i].offset(), exportedData);
          }
        } else {
          // NOTE: All fields will use the same formatter!
          formatter = getFormatter(method, fieldsAnnotation);
          formatdata = getFormatInstructions(method, fieldsAnnotation);

          int len = Array.getLength(valueObject);
          if (len != fieldsAnnotation.count()) {
            throw new FixedFormatException(format("Length mismatch for %s, expected %d but got %d elements!", stripMethodPrefix(method.getName()), fieldsAnnotation.count(), len));
          }

          for (int i = 0; i < fieldsAnnotation.count(); i++) {
            Object item = Array.get(valueObject, i);
            String exportedData = exportDataAccordingFieldAnnotation(formatter, formatdata, item);
            foundData.put(fieldsAnnotation.offset() + i * fieldsAnnotation.length(), exportedData);
          }
        }
      }
    }

    Set<Integer> sortedoffsets = foundData.keySet();
    for (Integer offset : sortedoffsets) {
      String data = foundData.get(offset);
      appendData(result, record.paddingChar(), offset, data);
    }

    if (record.length() != -1) { //pad with paddingchar
      while (result.length() < record.length()) {
        result.append(record.paddingChar());
      }
    }
    return result.toString();
  }

  @SuppressWarnings({"unchecked"})
  private FixedFormatter getFormatter(Method method, Fields fields) {
    Class<?> dataType = method.getReturnType();

    if (dataType.isArray()) {
      dataType = dataType.getComponentType();
    }

    FormatContext<?> context = getFormatContext(dataType, fields);
    return getFixedFormatterInstance(context.getFormatter(), context);
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
  protected <T> Object readDataAccordingFieldAnnotation(Class<T> clazz, String data, Method method, Fields fields, Class datatype, int index) throws ParseException {
    FormatContext context = getFormatContext(datatype, fields, index);
    FixedFormatter formatter = getFixedFormatterInstance(context.getFormatter(), context);
    FormatInstructions formatdata = getFormatInstructions(method, fields);
    return readDataAccordingFieldAnnotation(clazz, data, method, formatter, formatdata, context, datatype);
  }

  @SuppressWarnings({"unchecked"})
  protected <T> Object readDataAccordingFieldAnnotation(Class<T> clazz, String data, Method method, Field fieldAnno, Class datatype) throws ParseException {
    FormatContext context = getFormatContext(datatype, fieldAnno);
    FixedFormatter formatter = getFixedFormatterInstance(context.getFormatter(), context);
    FormatInstructions formatdata = getFormatInstructions(method, fieldAnno);
    return readDataAccordingFieldAnnotation(clazz, data, method, formatter, formatdata, context, datatype);
  }

  @SuppressWarnings({"unchecked"})
  protected <T> Object readDataAccordingFieldAnnotation(Class<T> clazz, String data, Method method, FixedFormatter formatter, FormatInstructions formatdata, FormatContext context, Class datatype) throws ParseException {
    String dataToParse = fetchData(data, formatdata, context);

    Object loadedData;

    Annotation recordAnno = datatype.getAnnotation(Record.class);
    if (recordAnno != null) {
      loadedData = load(datatype, dataToParse);
    } else {
      try {
        loadedData = formatter.parse(dataToParse, formatdata);
      } catch (RuntimeException e) {
        throw new ParseException(data, dataToParse, clazz, method, context, formatdata, e);
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("the loaded data[" + loadedData + "]");
    }
    return loadedData;
  }

  private Class getDatatype(Method method, Field fieldAnno) {
    Class datatype;
    if (followsBeanStandard(method)) {
      datatype = method.getReturnType();
    } else {
      throw new FixedFormatException(format("Cannot annotate method %s, with %s annotation. %s annotations must be placed on methods starting with 'get' or 'is'", method.getName(), fieldAnno.getClass().getName(), fieldAnno.getClass().getName()));
    }
    return datatype;
  }

  private FixedFormatter getFormatter(Method method, Field field) {
    Class<?> dataType = method.getReturnType();

    if (dataType.isArray()) {
      dataType = dataType.getComponentType();
    }

    FormatContext<?> context = getFormatContext(dataType, field);
    return getFixedFormatterInstance(context.getFormatter(), context);
  }

  @SuppressWarnings({"unchecked"})
  private <T> String exportDataAccordingFieldAnnotation(FixedFormatter formatter, FormatInstructions formatdata, Object valueObject) {
    String result;

    // Recursivly follow if the valueObject is annotated as a record.
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
  private <T> FormatContext<T> getFormatContext(Class<T> dataType, Field field) {
    if (field == null) {
      return null;
    }
    return new FormatContext(field.offset(), dataType, field.formatter());
  }

  @SuppressWarnings({"unchecked"})
  private <T> FormatContext<T> getFormatContext(Class<T> dataType, Fields fields) {
    return getFormatContext(dataType, fields, 0);
  }

  @SuppressWarnings({"unchecked"})
  private <T> FormatContext<T> getFormatContext(Class<T> dataType, Fields fields, int index) {
    if (fields == null) {
      return null;
    }
    return new FormatContext(fields.offset() + fields.length() * index, dataType, fields.formatter());
  }

  private FormatInstructions getFormatInstructions(Method method, Field fieldAnno) {
    FixedFormatPatternData patternData = getFixedFormatPatternData(method.getAnnotation(FixedFormatPattern.class));
    FixedFormatBooleanData booleanData = getFixedFormatBooleanData(method.getAnnotation(FixedFormatBoolean.class));
    FixedFormatNumberData numberData = getFixedFormatNumberData(method.getAnnotation(FixedFormatNumber.class));
    FixedFormatDecimalData decimalData = getFixedFormatDecimalData(method.getAnnotation(FixedFormatDecimal.class));
    return new FormatInstructions(fieldAnno.length(), fieldAnno.align(), fieldAnno.paddingChar(), patternData, booleanData, numberData, decimalData);
  }

  private FormatInstructions getFormatInstructions(Method method, Fields fields) {
    FixedFormatPatternData patternData = getFixedFormatPatternData(method.getAnnotation(FixedFormatPattern.class));
    FixedFormatBooleanData booleanData = getFixedFormatBooleanData(method.getAnnotation(FixedFormatBoolean.class));
    FixedFormatNumberData numberData = getFixedFormatNumberData(method.getAnnotation(FixedFormatNumber.class));
    FixedFormatDecimalData decimalData = getFixedFormatDecimalData(method.getAnnotation(FixedFormatDecimal.class));
    return new FormatInstructions(fields.length(), fields.align(), fields.paddingChar(), patternData, booleanData, numberData, decimalData);
  }

  private FixedFormatPatternData getFixedFormatPatternData(FixedFormatPattern annotation) {
    FixedFormatPatternData result;
    if (annotation != null) {
      result = new FixedFormatPatternData(annotation.value());
    } else {
      result = FixedFormatPatternData.DEFAULT;
    }
    return result;
  }

  private FixedFormatBooleanData getFixedFormatBooleanData(FixedFormatBoolean annotation) {
    FixedFormatBooleanData result;
    if (annotation != null) {
      result = new FixedFormatBooleanData(annotation.trueValue(), annotation.falseValue());
    } else {
      result = FixedFormatBooleanData.DEFAULT;
    }
    return result;
  }

  private FixedFormatNumberData getFixedFormatNumberData(FixedFormatNumber annotation) {
    FixedFormatNumberData result;
    if (annotation != null) {
      result = new FixedFormatNumberData(annotation.sign(), annotation.positiveSign(), annotation.negativeSign());
    } else {
      result = FixedFormatNumberData.DEFAULT;
    }
    return result;
  }

  private FixedFormatDecimalData getFixedFormatDecimalData(FixedFormatDecimal annotation) {
    FixedFormatDecimalData result;
    if (annotation != null) {
      result = new FixedFormatDecimalData(annotation.decimals(), annotation.useDecimalDelimiter(), annotation.decimalDelimiter(), RoundingMode.valueOf(annotation.roundingMode()));
    } else {
      result = FixedFormatDecimalData.DEFAULT;
    }
    return result;
  }

  private boolean followsBeanStandard(Method method) {
    String methodName = method.getName();
    return methodName.startsWith("get") || methodName.startsWith("is");
  }

  private static boolean isWrapped(Fields fields) {
    if (fields.value() != null && fields.value().length > 0) {
      return true;
    } else {
      return false;
    }
  }

  private void checkValid(Fields fields) {
    if (isWrapped(fields)) {
      if (fields.value().length == 0) {
        throw new FixedFormatException("Fields annotation must contain minimum one Field annotation");
      }
      return;
    }
    if (fields.offset() < 1) {
      throw new FixedFormatException("Fields annotation must have positive non-zero offset (or contain Field annotations)");
    }
    if (fields.length() < 1) {
      throw new FixedFormatException("Fields annotation must have positive non-zero length (or contain Field annotations)");
    }
    if (fields.count() < 1) {
      throw new FixedFormatException("Fields annotation must have positive non-zero count (or contain Field annotations)");
    }
  }
}
