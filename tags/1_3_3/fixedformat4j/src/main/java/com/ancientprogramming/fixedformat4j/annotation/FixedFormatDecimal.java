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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;

/**
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface FixedFormatDecimal {

  public static final int DECIMALS = 2;
  public static final boolean USE_DECIMAL_DELIMITER = false;
  public static final char DECIMAL_DELIMITER = '.';
  public static final int ROUNDING_MODE = BigDecimal.ROUND_HALF_UP;

  int decimals() default DECIMALS;

  boolean useDecimalDelimiter() default USE_DECIMAL_DELIMITER;

  char decimalDelimiter() default DECIMAL_DELIMITER;

  int roundingMode() default ROUNDING_MODE;
  
}
