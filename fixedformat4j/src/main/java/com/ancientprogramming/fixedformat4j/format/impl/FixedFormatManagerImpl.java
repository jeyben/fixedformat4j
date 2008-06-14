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
import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.fetchData;
import static com.ancientprogramming.fixedformat4j.format.FixedFormatUtil.getFixedFormatterInstance;
import com.ancientprogramming.fixedformat4j.format.*;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static java.lang.String.format;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

/**
 * Load and export data to and from fixedformat
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class FixedFormatManagerImpl implements FixedFormatManager {

  private static final Log LOG = LogFactory.getLog(FixedFormatManagerImpl.class);

  public <T> T load(Class<T> fixedFormatRecordClass, String data) {
    HashMap<String, Object> foundData = new HashMap<String, Object>();
    //assert the record is marked with a Record
    getAndAssertRecordAnnotation(fixedFormatRecordClass);

    //create instance to set data into
    T instance;
    try {
      Constructor<T> constructor = fixedFormatRecordClass.getConstructor();
      instance = constructor.newInstance();
    } catch (NoSuchMethodException e) {
      throw new FixedFormatException(format("%s is missing a default constructor which is nessesary to be loaded through %s", fixedFormatRecordClass.getName(), getClass().getName()));
    } catch (Exception e) {
      throw new FixedFormatException(format("unable to create instance of %s", fixedFormatRecordClass.getName()), e);
    }

    //look for setter annotations and read data from the 'data' string
    Method[] allMethods = fixedFormatRecordClass.getMethods();
    for (Method method : allMethods) {
      String methodName = stripMethodPrefix(method.getName());
      Field fieldAnnotation = method.getAnnotation(Field.class);
      Fields fieldsAnnotation = method.getAnnotation(Fields.class);
      if (fieldAnnotation != null) {
        Object loadedData = readDataAccordingFieldAnnotation(fixedFormatRecordClass, data, method, fieldAnnotation);
        foundData.put(methodName, loadedData);
      } else if (fieldsAnnotation != null) {
        //assert that the fields annotation contains minimum one field anno
        if (fieldsAnnotation.value() == null || fieldsAnnotation.value().length == 0) {
          throw new FixedFormatException(format("%s annotation must contain minimum one %s annotation", Fields.class.getName(), Field.class.getName()));
        }
        Object loadedData = readDataAccordingFieldAnnotation(fixedFormatRecordClass, data, method, fieldsAnnotation.value()[0]);
        foundData.put(methodName, loadedData);
      }
    }

    Set<String> keys = foundData.keySet();
    for (String key : keys) {
      String setterMethodName = "set" + key;

      Object foundDataObj = foundData.get(key);
      if (foundDataObj != null) {
        Class datatype = foundData.get(key).getClass();
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

  public <T> String export(String existingData, T fixedFormatRecord) {
    StringBuffer result = new StringBuffer(existingData);
    Record record = getAndAssertRecordAnnotation(fixedFormatRecord.getClass());

    Class fixedFormatRecordClass = fixedFormatRecord.getClass();
    HashMap<Integer, String> foundData = new HashMap<Integer, String>(); // hashmap containing offset and data to write
    Method[] allMethods = fixedFormatRecordClass.getMethods();
    for (Method method : allMethods) {
      Field fieldAnnotation = method.getAnnotation(Field.class);
      Fields fieldsAnnotation = method.getAnnotation(Fields.class);
      if (fieldAnnotation != null) {
        String exportedData = exportDataAccordingFieldAnnotation(fixedFormatRecord, method, fieldAnnotation);
        foundData.put(fieldAnnotation.offset(), exportedData);
      } else if (fieldsAnnotation != null) {
        Field[] fields = fieldsAnnotation.value();
        for (Field field : fields) {
          String exportedData = exportDataAccordingFieldAnnotation(fixedFormatRecord, method, field);
          foundData.put(field.offset(), exportedData);
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

  private <T> Object readDataAccordingFieldAnnotation(Class<T> clazz, String data, Method method, Field fieldAnno) throws ParseException {
    Class datatype = getDatatype(method, fieldAnno);

    FormatContext context = getFormatContext(datatype, fieldAnno);
    FixedFormatter formatter = getFixedFormatterInstance(context.getFormatter(), context);
    FormatInstructions formatdata = getFormatInstructions(method, fieldAnno);

    String dataToParse = fetchData(data, formatdata, context);
    Object loadedData;
    try {
    loadedData = formatter.parse(dataToParse, formatdata);
    } catch (RuntimeException e) {
      throw new ParseException(data, dataToParse, clazz, method, context, formatdata, e);
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

  private <T> String exportDataAccordingFieldAnnotation(T fixedFormatRecord, Method method, Field fieldAnno) {
    String result;
    Class datatype = getDatatype(method, fieldAnno);

    FormatContext context = getFormatContext(datatype, fieldAnno);
    FixedFormatter formatter = getFixedFormatterInstance(context.getFormatter(), context);
    FormatInstructions formatdata = getFormatInstructions(method, fieldAnno);
    Object valueObject;
    try {
      valueObject = method.invoke(fixedFormatRecord);
    } catch (Exception e) {
      throw new FixedFormatException(format("could not invoke method %s.%s(%s)", fixedFormatRecord.getClass().getName(), method.getName(), datatype), e);
    }
    result = formatter.format(valueObject, formatdata);
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


  private FormatContext getFormatContext(Class datatype, Field fieldAnno) {
    FormatContext context = null;
    if (fieldAnno != null) {
      context = new FormatContext(fieldAnno.offset(), datatype, fieldAnno.formatter());
    }
    return context;

  }

  private FormatInstructions getFormatInstructions(Method method, Field fieldAnno) {
    FixedFormatPatternData patternData = getFixedFormatPatternData(method.getAnnotation(FixedFormatPattern.class));
    FixedFormatBooleanData booleanData = getFixedFormatBooleanData(method.getAnnotation(FixedFormatBoolean.class));
    FixedFormatNumberData numberData = getFixedFormatNumberData(method.getAnnotation(FixedFormatNumber.class));
    FixedFormatDecimalData decimalData = getFixedFormatDecimalData(method.getAnnotation(FixedFormatDecimal.class));
    return new FormatInstructions(fieldAnno.length(), fieldAnno.align(), fieldAnno.paddingChar(), patternData, booleanData, numberData, decimalData);
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
      result = new FixedFormatDecimalData(annotation.decimals(), annotation.useDecimalDelimiter(), annotation.decimalDelimiter());
    } else {
      result = FixedFormatDecimalData.DEFAULT;
    }
    return result;
  }

  private boolean followsBeanStandard(Method method) {
    String methodName = method.getName();
    return methodName.startsWith("get") || methodName.startsWith("is");
  }
}
