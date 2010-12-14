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
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
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
   * @return The direction of the padding. Defaults to {@link Align#RIGHT}.
   */
  Align align() default Align.LEFT;

  /**
   * The character to pad with if the length is longer than the formatted data
   * @return the padding character
   */
  char paddingChar() default ' ';

  Class<? extends FixedFormatter> formatter() default ByTypeFormatter.class;

}
