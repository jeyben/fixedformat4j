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
package com.ancientprogramming.fixedformat4j.record.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Fields;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatAnnotationUtil;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatData;
import com.ancientprogramming.fixedformat4j.format.FixedFormatMetadata;
import static com.ancientprogramming.fixedformat4j.format.FixedFormatProcessor.*;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import com.ancientprogramming.fixedformat4j.record.FixedFormatManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static java.lang.String.format;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

/**
 * reads and writes data to and from fixedformat
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class FixedFormatManagerImpl implements FixedFormatManager {

  private static final Log LOG = LogFactory.getLog(FixedFormatManagerImpl.class);

  public <T> T load(Class<T> fixedFormatRecordClass, String data) {
    HashMap<String, Object> foundData = new HashMap<String, Object>();
    //assert the record is marked with a Record
    Record recordAnno = FixedFormatAnnotationUtil.getAnnotation(fixedFormatRecordClass, Record.class);
    if (recordAnno == null) {
      throw new FixedFormatException(format("%s has to be marked with the record annotation to be loaded", fixedFormatRecordClass.getName()));
    }

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
      if (!foundData.containsKey(methodName)) {
        Field fieldAnnotation = method.getAnnotation(Field.class);
        Fields fieldsAnnotation = method.getAnnotation(Fields.class);
        if (fieldAnnotation != null) {
          Object loadedData = readDataAccordingToTheFieldAnnotation(data, method, fieldAnnotation);
          foundData.put(methodName, loadedData);
        } else if (fieldsAnnotation != null) {
          //assert that the fields annotation contains minimum one field anno
          if (fieldsAnnotation.value() == null || fieldsAnnotation.value().length == 0) {
            throw new FixedFormatException(format("%s annotation must contain minimum one %s annotation", Fields.class.getName(), Field.class.getName()));
          }
          Object loadedData = readDataAccordingToTheFieldAnnotation(data, method, fieldsAnnotation.value()[0]);
          foundData.put(methodName, loadedData);
        }
      }
    }

    Set<String> keys = foundData.keySet();
    for (String key : keys) {
      String setterMethodName = "set" + key;
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
    return instance;
  }

  private <T> Object readDataAccordingToTheFieldAnnotation(String data, Method method, Field fieldAnno) {
    Class datatype = null;
    if (isSetter(method)) {
      datatype = method.getParameterTypes()[0];
    } else if (isGetter(method)) {
      datatype = method.getReturnType();
    }
    FixedFormatMetadata metadata = getMetadata(datatype, fieldAnno);
    FixedFormatter formatter = getFixedFormatterInstance(metadata.getFormatter(), metadata);
    FixedFormatData formatdata = getFormatData(method, fieldAnno);

    assertIsPatternRequired(formatdata, metadata, formatter);
    assertIsBooleanRequired(formatdata, metadata, formatter);
    assertIsDecimalRequired(formatdata, metadata, formatter);
    Object loadedData = formatter.parse(fetchData(new StringBuffer(data), formatdata, metadata), formatdata);
    if (LOG.isDebugEnabled()) {
      LOG.debug("the loaded data[" + loadedData + "]");
    }
    return loadedData;
  }

  public <T> String write(T fixedFormatRecord) {
    return "";
  }


  private String stripMethodPrefix(String name) {
    return name.substring(3);
  }


  private FixedFormatMetadata getMetadata(Class datatype, Field fieldAnno) {
    FixedFormatMetadata metadata = null;
    if (fieldAnno != null) {
      metadata = new FixedFormatMetadata(fieldAnno.offset(), datatype, fieldAnno.formatter());
    }
    return metadata;

  }

  private FixedFormatData getFormatData(Method method, Field fieldAnno) {
    FixedFormatPatternData patternData = getFixedFormatPatternData(method.getAnnotation(FixedFormatPattern.class));
    FixedFormatBooleanData booleanData = getFixedFormatBooleanData(method.getAnnotation(FixedFormatBoolean.class));
    FixedFormatDecimalData decimalData = getFixedFormatDecimalData(method.getAnnotation(FixedFormatDecimal.class));
    return new FixedFormatData(fieldAnno.length(), fieldAnno.align(), fieldAnno.paddingChar(), patternData, booleanData, decimalData);
  }

  private FixedFormatPatternData getFixedFormatPatternData(FixedFormatPattern annotation) {
    FixedFormatPatternData result = null;
    if (annotation != null) {
      result = new FixedFormatPatternData(annotation.value());
    }
    return result;
  }

  private FixedFormatBooleanData getFixedFormatBooleanData(FixedFormatBoolean annotation) {
    FixedFormatBooleanData result = null;
    if (annotation != null) {
      result = new FixedFormatBooleanData(annotation.trueValue(), annotation.falseValue());
    }
    return result;
  }

  private FixedFormatDecimalData getFixedFormatDecimalData(FixedFormatDecimal annotation) {
    FixedFormatDecimalData result = null;
    if (annotation != null) {
      result = new FixedFormatDecimalData(annotation.decimals(), annotation.useDecimalDelimiter(), annotation.decimalDelimiter());
    }
    return result;
  }

  private boolean isSetter(Method method) {
    return method.getName().startsWith("set");
  }

  private boolean isGetter(Method method) {
    return method.getName().startsWith("get");
  }
}
