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
package com.ancientprogramming.fixedformat4j.annotation;

import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.impl.ByTypeFormatter;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * This annotation descibes how a setter/getter pairs should be formatted by the fixedFormatManager.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Field {

  /**
   * A one based offset to insert data at in a record.
   * @return the offset as an int
   */
  int offset();

  /**
   * The length of the formatted field
   * @return the length as an int
   */
  int length();

  /**
   * @return The direction of the padding. Defaults to {@link Align#LEFT}.
   */
  Align align() default Align.LEFT;

  /**
   * The character to pad with if the length is longer than the formatted data
   * @return the padding character
   */
  char paddingChar() default ' ';

  Class<? extends FixedFormatter<?>> formatter() default ByTypeFormatter.class;

  /**
   * Number of consecutive repetitions of this field.
   * When greater than 1, the getter/setter must use an array or an ordered {@link java.util.Collection}
   * (e.g. {@code List}, {@code Set}, {@code SortedSet}). Each repetition occupies
   * {@code length} characters, starting at {@code offset + length * index}.
   *
   * @return the repetition count, must be &gt;= 1
   */
  int count() default 1;

  /**
   * Controls export behaviour when the actual collection/array size differs from {@link #count()}.
   * Only relevant when {@link #count()} &gt; 1.
   * <ul>
   *   <li>{@code true} (default) — throws a {@link com.ancientprogramming.fixedformat4j.exception.FixedFormatException}</li>
   *   <li>{@code false} — logs a warning and exports {@code min(count, actualSize)} elements</li>
   * </ul>
   *
   * @return whether to throw on size mismatch during export
   */
  boolean strictExportCount() default true;

}
