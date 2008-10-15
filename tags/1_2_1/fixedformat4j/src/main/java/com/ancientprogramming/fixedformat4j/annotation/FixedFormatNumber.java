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
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.1.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface FixedFormatNumber {

  //todo: this gives exception
  //annotation com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber is missing <clinit>
  //public static final Sign DEFAULT_SIGN = Sign.NOSIGN;

  public static final char DEFAULT_POSITIVE_SIGN = '+';
  public static final char DEFAULT_NEGATIVE_SIGN = '-';


  Sign sign() default Sign.NOSIGN;

  char positiveSign() default DEFAULT_POSITIVE_SIGN;

  char negativeSign() default DEFAULT_NEGATIVE_SIGN;
}
