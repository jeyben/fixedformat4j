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
package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Sign;
import com.ancientprogramming.fixedformat4j.format.AbstractFixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

/**
 * Apply signing to values
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.1.0
 */
public abstract class AbstractNumberFormatter<T> extends AbstractFixedFormatter<T> {

  /**
   * Override and applies signing instead of align.
   *
   * @param value the value
   * @param instructions the instructions
   * @return the parsed object
   */
  public T parse(String value, FormatInstructions instructions) {
    T result = null;
    if (value != null) {
      Sign signing = instructions.getFixedFormatNumberData().getSigning();
      String rawString = signing.remove(value, instructions);
      result = asObject(rawString, instructions);
    }
    return result;
  }

  /**
     * Override and applies signing instead of align.
     *
     * @param obj the object to format
     * @param instructions the instructions
     * @return the raw value
     */
    public String format(T obj, FormatInstructions instructions) {
      return instructions.getFixedFormatNumberData().getSigning().apply(asString(obj, instructions), instructions);
    }
}
