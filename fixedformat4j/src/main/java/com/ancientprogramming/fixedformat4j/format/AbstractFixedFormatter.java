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
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public abstract class AbstractFixedFormatter<T> implements FixedFormatter<T> {
  /**
   * {@inheritDoc}
   * <p>
   * Strips padding from {@code value} according to the alignment and padding char defined in
   * {@code instructions}, then delegates to {@link #asObject(String, FormatInstructions)}.
   * Returns {@code null} when {@code value} is {@code null}.
   */
  public T parse(String value, FormatInstructions instructions) {
    T result = null;
    if (value != null) {
      result = asObject(stripPadding(value, instructions), instructions);
    }
    return result;
  }

  /**
   * Strips padding characters from {@code value} before it is passed to {@link #asObject}.
   *
   * <p>This is the extension point for subclasses that need to customise pre-parse
   * padding removal.
   *
   * @param value        the raw field string extracted from the fixed-width record
   * @param instructions formatting metadata (alignment, padding character, length, &hellip;)
   * @return the value with padding removed, ready to be converted by {@link #asObject}
   */
  protected String stripPadding(String value, FormatInstructions instructions) {
    return instructions.getAlignment().remove(value, instructions.getPaddingChar());
  }

  /**
   * {@inheritDoc}
   * <p>
   * Converts {@code value} to its string representation via
   * {@link #asString(Object, FormatInstructions)}, then applies padding and alignment as defined
   * in {@code instructions}.
   */
  public String format(T value, FormatInstructions instructions) {
    return instructions.getAlignment().apply(asString(value, instructions), instructions.getLength(), instructions.getPaddingChar());
  }

  /**
   * Converts the trimmed string {@code string} to an instance of {@code T}.
   * Padding has already been removed from {@code string} before this method is called.
   *
   * @param string       the raw (padding-stripped) field value
   * @param instructions formatting instructions for the field
   * @return the parsed value, or {@code null} if the field is empty
   */
  public abstract T asObject(String string, FormatInstructions instructions);

  /**
   * Converts {@code obj} to its raw string representation before padding is applied.
   *
   * @param obj          the value to convert; may be {@code null}
   * @param instructions formatting instructions for the field
   * @return the string representation of {@code obj}
   */
  public abstract String asString(T obj, FormatInstructions instructions);
}
