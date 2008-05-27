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

import com.ancientprogramming.fixedformat4j.format.AbstractFixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.annotation.Sign;
import org.apache.commons.lang.StringUtils;

/**
 * Apply signing according to
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.1.0
 */
public abstract class AbstractNumberFormatter extends AbstractFixedFormatter {
  public Object parse(String value, FormatInstructions instructions) {
    Object result = null;
    if (value != null) {
      Sign signing = instructions.getFixedFormatNumberData().getSigning();
      String rawString = signing.remove(value, instructions);
      result = asObject(rawString, instructions);
    }
    return result;
  }

  public String format(Object value, FormatInstructions instructions) {
    return instructions.getFixedFormatNumberData().getSigning().apply(asString(value, instructions), instructions);
  }

  public String stripSigningForPositiveAndZeroNumbers(String value) {
    String sign = StringUtils.substring(value, 0, 1);
    String unsignedValue = StringUtils.substring(value, 1);
    String result = value;
    
    if (StringUtils.isEmpty(unsignedValue) || "+".equals(sign)) {
      result = unsignedValue;
    }
    return StringUtils.isEmpty(result) ? "0" : result;
  }

}
