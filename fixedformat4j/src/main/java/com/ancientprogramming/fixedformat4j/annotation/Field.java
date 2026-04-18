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
   * Sentinel value for {@link #nullChar()} meaning "not configured". When {@code nullChar()}
   * equals this value, null-aware load/export is disabled and behavior is identical to
   * pre-1.7.1 releases.
   *
   * @since 1.7.1
   */
  char UNSET_NULL_CHAR = '\0';

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
   * The direction of the padding for this field. Defaults to {@link Align#INHERIT}, which means
   * the alignment is inherited from {@link Record#align()} on the enclosing record class.
   * Specify an explicit value to override the record-level default for this field only.
   *
   * @return the field alignment, or {@link Align#INHERIT} to defer to the record default
   * @since 1.7.1 default changed from {@link Align#LEFT} to {@link Align#INHERIT}
   */
  Align align() default Align.INHERIT;

  /**
   * The character to pad with if the length is longer than the formatted data
   * @return the padding character
   */
  char paddingChar() default ' ';

  /**
   * Opt-in sentinel used to represent a {@code null} value in the fixed-width field.
   * <p>
   * Activation rule: null-aware handling fires only when {@code nullChar() != paddingChar()}.
   * When active:
   * <ul>
   *   <li>On load, a slice whose characters all equal {@code nullChar} yields {@code null}
   *       (the setter is not invoked, so primitive fields keep their JVM default).</li>
   *   <li>On export, a {@code null} getter value is emitted as {@code length} copies of
   *       {@code nullChar}, bypassing the formatter entirely.</li>
   * </ul>
   * The default value {@code '\0'} is a sentinel that can never appear in a regular
   * fixed-width payload, so existing records (which do not set this attribute) retain
   * their pre-existing behavior.
   * <p>
   * For repeating fields ({@code count > 1}), the check is applied <em>per element</em>:
   * each element slot is evaluated independently. Primitive array element types (e.g.
   * {@code int[]}) cannot represent {@code null} and are unaffected by this attribute.
   *
   * @return the character that denotes a null field; defaults to {@link #UNSET_NULL_CHAR} (inactive)
   * @since 1.7.1
   */
  char nullChar() default UNSET_NULL_CHAR;

  /**
   * The formatter class to use for this field.
   * Defaults to {@link ByTypeFormatter}, which selects the formatter automatically based on the
   * getter's return type. Override only when custom formatting logic is required.
   *
   * @return the formatter class
   */
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
  boolean strictCount() default true;

}
