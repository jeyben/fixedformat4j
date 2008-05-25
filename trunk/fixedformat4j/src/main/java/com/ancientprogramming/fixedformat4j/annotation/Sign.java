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

import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import org.apache.commons.lang.StringUtils;

/**
 * Apply signing. Will always chop most significant digit incase the
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.1.0
 */
public enum Sign {
  PREPEND {
    public String apply(String value, FormatInstructions instructions) {
      String sign = StringUtils.substring(value, 0, 1);
      if ("-".equals(sign)) {
        value = StringUtils.substring(value, 1);
      } else {
        sign = "+";
      }
      String result = instructions.getAlignment().apply(value, instructions.getLength(), instructions.getPaddingChar());
      return sign + StringUtils.substring(result, 1);
    }

    public String remove(String value, FormatInstructions instructions) {
      String sign = StringUtils.substring(value, 0, 1);
      String valueWithoutSign;
      valueWithoutSign = StringUtils.substring(value, 1);
      String result = instructions.getAlignment().remove(valueWithoutSign, instructions.getPaddingChar());
      return sign + result;
    }
  };


  public abstract String apply(String value, FormatInstructions instruction);

  public abstract String remove(String value, FormatInstructions instruction);
}
