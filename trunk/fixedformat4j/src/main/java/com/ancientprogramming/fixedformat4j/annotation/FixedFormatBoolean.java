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
 * Define representations for {@link Boolean#TRUE} and {@link Boolean#FALSE}
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface FixedFormatBoolean {

  /**
   * The default <code>true</code> value
   */
  public static final String TRUE_VALUE = "T";

  /**
   * The default <code>false</code> value
   */
  public static final String FALSE_VALUE = "F";

  /**
   * The string to map a boolean true value to.
   * @return contains the string representation of a <code>true</code> value
   */
  String trueValue() default TRUE_VALUE;

  /**
   * The string to map a boolean false value to.
   * @return contains the string representation of a <code>false</code> value
   */
  String falseValue() default FALSE_VALUE;
}
