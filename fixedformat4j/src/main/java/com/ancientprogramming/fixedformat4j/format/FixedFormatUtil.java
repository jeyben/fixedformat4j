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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * Utility class used when loading and exporting to and from fixedformat data.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class FixedFormatUtil {

  private static final Logger LOG = LoggerFactory.getLogger(FixedFormatUtil.class);

  /**
   * Fetches the slice of {@code record} that corresponds to the field described by
   * {@code instructions} and {@code context}.
   *
   * @param record       the full fixed-width record string
   * @param instructions the field's formatting instructions (supplies the field length)
   * @param context      the format context (supplies the 1-based field offset)
   * @return the extracted field substring, or {@code null} if {@code record} is shorter than
   *         the requested offset
   */
  public static String fetchData(String record, FormatInstructions instructions, FormatContext<?> context) {
    String result;
    int offset = context.getOffset() - 1;
    int length = instructions.getLength();
    if (length == -1) {
      // rest-of-line: capture from offset to end of line verbatim
      if (offset <= record.length()) {
        result = record.substring(offset);
      } else {
        result = null;
        LOG.debug(format("Could not fetch rest-of-line data: recordlength[%s] is shorter than the requested offset[%s]. Returning null", record.length(), offset));
      }
      if (LOG.isDebugEnabled()) {
        LOG.debug(format("fetched '%s' from record", result));
      }
      return result;
    }
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

  /**
   * Creates an instance of the given {@code formatterClass}, first trying a single-argument
   * constructor that accepts a {@link FormatContext}, then falling back to a no-arg constructor.
   *
   * @param formatterClass the formatter class to instantiate
   * @param context        the {@link FormatContext} passed to the single-argument constructor attempt
   * @param <T>            the value type handled by the formatter
   * @return a ready-to-use formatter instance
   * @throws FixedFormatException if neither constructor is available
   */
  public static <T> FixedFormatter<T> getFixedFormatterInstance(Class<? extends FixedFormatter<T>> formatterClass, FormatContext<?> context) {
    FixedFormatter<T> formatter = getFixedFormatterInstance(formatterClass, context.getClass(), context);
    if (formatter == null) {
      formatter = getFixedFormatterInstance(formatterClass, null, null);
    }
    if (formatter == null) {
      throw new FixedFormatException(format("could not create instance of [%s] because the class has no default constructor and no constructor with %s as argument.", formatterClass.getName(), FormatContext.class.getName()));
    }
    return formatter;
  }

  /**
   * Creates an instance of {@code formatterClass} using either a single-argument constructor
   * (when both {@code paramType} and {@code paramValue} are non-null) or a no-arg constructor.
   * Returns {@code null} when the requested constructor does not exist.
   *
   * @param formatterClass the formatter class to instantiate
   * @param paramType      the constructor parameter type; {@code null} to force the no-arg path
   * @param paramValue     the constructor argument value; {@code null} to force the no-arg path
   * @param <T>            the value type handled by the formatter
   * @return a formatter instance, or {@code null} if the matching constructor is absent
   * @throws FixedFormatException if a matching constructor exists but instantiation fails
   */
  public static <T> FixedFormatter<T> getFixedFormatterInstance(Class<? extends FixedFormatter<T>> formatterClass, Class<?> paramType, FormatContext<?> paramValue) {
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
