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
package com.ancientprogramming.fixedformat4j.format;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;

/**
 * Utility class used when loading and exporting to and from fixedformat data.
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class FixedFormatUtil {

  private static final Log LOG = LogFactory.getLog(FixedFormatUtil.class);

  /**
   * Fetch data from the record string according to the {@link FormatInstructions} and {@link FormatContext}
   * @param record the string to fetch from
   * @param instructions the fixed
   * @param context
   * @return
   */
  public static String fetchData(String record, FormatInstructions instructions, FormatContext context) {
    String result;
    int offset = context.getOffset() - 1;
    int length = instructions.getLength();
    if (record.length() >= offset + length) {
      result = record.substring(offset, offset + length);
    } else {
      result = null;
      LOG.warn("Could not fetch data from record as the recordlength[" + record.length() + "] was shorter than the requested offset[" + offset + "] + length[" + length + "] of the request data. Returning null");
    }
    return result;
  }

  public static FixedFormatter getFixedFormatterInstance(Class<? extends FixedFormatter> formatterClass, FormatContext context) {
    FixedFormatter formatter = getFixedFormatterInstance(formatterClass, context.getClass(), context);
    if (formatter == null) {
      formatter = getFixedFormatterInstance(formatterClass, null, null);
    }
    if (formatter == null) {
      throw new FixedFormatException("could not create instance of [" + formatterClass.getName() + "] because the class has no default constructor and no constructor with " + FormatContext.class.getName() + " as argument.");
    }
    return formatter;
  }

  public static FixedFormatter getFixedFormatterInstance(Class<? extends FixedFormatter> formatterClass, Class paramType, FormatContext paramValue) {
    FixedFormatter result;
    if (paramType != null && paramValue != null) {
      try {
        result = formatterClass.getConstructor(paramType).newInstance(paramValue);
      } catch (NoSuchMethodException e) {
        result = null;
      } catch (Exception e) {
        throw new FixedFormatException("Could not create instance with one argument constructor", e);
      }
    } else {
      try {
        result = formatterClass.getConstructor().newInstance();
      } catch (NoSuchMethodException e) {
        result = null;
      } catch (Exception e) {
        throw new FixedFormatException("Could not create instance with no arg constructor", e);
      }
    }
    return result;
  }
}
