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

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import static java.lang.String.format;

/**
 * Utility class used when loading and exporting to and from fixedformat data.
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class FixedFormatUtil {

  private static final Log LOG = LogFactory.getLog(FixedFormatUtil.class);

  /**
   * Fetch data from the record string according to the {@link FormatInstructions} and {@link FormatContext}
   * @param record the string to fetch from
   * @param instructions the fixed
   * @param context the context to fetch data in
   * @return the String data fetched from the record. Can be <code>null</code> if the record was shorter than the context expected
   */
  public static String fetchData(String record, FormatInstructions instructions, FormatContext context) {
    String result;
    int offset = context.getOffset() - 1;
    int length = instructions.getLength();
    if (record.length() >= offset + length) {
      result = record.substring(offset, offset + length);
    } else if (record.length() > offset) {
      //the field does contain data, but is not as long as the instructions tells.
      result = record.substring(offset, record.length());
      if (LOG.isDebugEnabled()) {
        LOG.info(format("The record field was not as long as expected by the instructions. Expected field to be %s long but it was %s.", length, record.length()));
      }
    } else {
      result = null;
      LOG.info(format("Could not fetch data from record as the recordlength[%s] was shorter than or equal to the requested offset[%s] of the request data. Returning null", record.length(), offset));
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug(format("fetched '%s' from record", result));
    }
    return result;
  }

  public static <T> FixedFormatter<T> getFixedFormatterInstance(Class<? extends FixedFormatter<T>> formatterClass, FormatContext context) {
    FixedFormatter<T> formatter = getFixedFormatterInstance(formatterClass, context.getClass(), context);
    if (formatter == null) {
      formatter = getFixedFormatterInstance(formatterClass, null, null);
    }
    if (formatter == null) {
      throw new FixedFormatException("could not create instance of [" + formatterClass.getName() + "] because the class has no default constructor and no constructor with " + FormatContext.class.getName() + " as argument.");
    }
    return formatter;
  }

  public static <T> FixedFormatter<T> getFixedFormatterInstance(Class<? extends FixedFormatter<T>> formatterClass, Class paramType, FormatContext paramValue) {
    FixedFormatter<T> result;
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
