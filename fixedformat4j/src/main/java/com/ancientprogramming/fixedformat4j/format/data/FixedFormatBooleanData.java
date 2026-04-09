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
package com.ancientprogramming.fixedformat4j.format.data;

import com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean;
import static com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean.*;

/**
 * Data object containing the exact same data as {@link FixedFormatBoolean} 
  *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class FixedFormatBooleanData {

  public static final FixedFormatBooleanData DEFAULT = new FixedFormatBooleanData(TRUE_VALUE, FALSE_VALUE);

  private String trueValue;

  private String falseValue;

  /**
   * Creates a boolean data object with the given string representations for {@code true} and
   * {@code false}.
   *
   * @param trueValue  the string that represents a {@code true} value in the fixed-width field
   * @param falseValue the string that represents a {@code false} value in the fixed-width field
   */
  public FixedFormatBooleanData(String trueValue, String falseValue) {
    this.trueValue = trueValue;
    this.falseValue = falseValue;
  }

  /**
   * Returns the string representation of {@code true} for this field.
   *
   * @return the true-value string
   */
  public String getTrueValue() {
    return trueValue;
  }

  /**
   * Returns the string representation of {@code false} for this field.
   *
   * @return the false-value string
   */
  public String getFalseValue() {
    return falseValue;
  }


  public String toString() {
    return String.format("FixedFormatBooleanData{trueValue='%s', falseValue='%s'}", trueValue, falseValue);
  }
}
