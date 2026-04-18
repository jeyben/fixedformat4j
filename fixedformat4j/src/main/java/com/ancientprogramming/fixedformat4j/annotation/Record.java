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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * Marks a class as a representation of a fixed format record
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Record {

  /**
   * The fixed length of the record. It means that the record will always be that long padded with {#paddingChar()}'s
   * @return the length of the record. -1 means no fixed length.
   */
  int length() default -1;

  /**
   * The char to pad with.
   * @return the char to pad with if the record is set to a fixed length;
   */
  char paddingChar() default ' ';

  /**
   * Default alignment applied to all fields in this record. Individual fields may override this
   * by setting {@link Field#align()} to any value other than {@link Align#INHERIT}.
   * Defaults to {@link Align#LEFT} to preserve pre-1.7.1 behaviour for records that do not
   * set this attribute.
   *
   * @return the record-level default alignment
   * @since 1.7.1
   */
  Align align() default Align.LEFT;
}
