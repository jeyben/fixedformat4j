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

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.AbstractFixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import org.apache.commons.lang.StringUtils;

/**
 * Formatter for {@link Boolean} data
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class BooleanFormatter extends AbstractFixedFormatter<Boolean> {

  public Boolean asObject(String string, FormatInstructions instructions) throws FixedFormatException {
    Boolean result = false;
    if (!StringUtils.isEmpty(string)) {
      if (instructions.getFixedFormatBooleanData().getTrueValue().equals(string)) {
        result = true;
      } else if (instructions.getFixedFormatBooleanData().getFalseValue().equals(string)) {
        result = false;
      } else {
        throw new FixedFormatException("Could not convert string[" + string + "] to boolean value according to booleanData[" + instructions.getFixedFormatBooleanData() + "]");
      }
    }
    return result;
  }

  public String asString(Boolean obj, FormatInstructions instructions) {
    String result = instructions.getFixedFormatBooleanData().getFalseValue();
    if (obj != null) {
      result = obj ? instructions.getFixedFormatBooleanData().getTrueValue() : instructions.getFixedFormatBooleanData().getFalseValue();
    }
    return result;
  }

}
