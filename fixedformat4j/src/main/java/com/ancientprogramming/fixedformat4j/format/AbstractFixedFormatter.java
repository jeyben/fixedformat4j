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

/**
 * Handles default formatting and parsing based on FixedFormatAnnotation values.
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public abstract class AbstractFixedFormatter<T> implements FixedFormatter<T> {
  public T parse(String value, FormatInstructions instructions) {
    T result = null;
    if (value != null) {
      instructions.getFixedFormatNumberData();
      result = asObject(getRemovePadding(value, instructions), instructions);
    }
    return result;
  }

  /**
   * Removes the padding characters defined in the instructions.
   * @param value the string to remove padding chars from
   * @param instructions the instructions containing the padding char
   * @return the remaining string value after padding chars was removed. The empty string if he <code>value</code> only contained adding chars.
   */
  String getRemovePadding(String value, FormatInstructions instructions) {
    return instructions.getAlignment().remove(value, instructions.getPaddingChar());
  }

  public String format(T value, FormatInstructions instructions) {
    return instructions.getAlignment().apply(asString(value, instructions), instructions.getLength(), instructions.getPaddingChar());
  }

  public abstract T asObject(String string, FormatInstructions instructions);

  public abstract String asString(T obj, FormatInstructions instructions);
}
