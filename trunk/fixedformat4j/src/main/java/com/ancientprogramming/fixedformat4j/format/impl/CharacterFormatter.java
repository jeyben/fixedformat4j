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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ancientprogramming.fixedformat4j.format.AbstractFixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

/**
 * Formatter for {@link Character} datatypes
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class CharacterFormatter extends AbstractFixedFormatter {

  private static final Log LOG = LogFactory.getLog(CharacterFormatter.class);

  public Object asObject(String string, FormatInstructions instructions) {
    Character result;
    if (string == null || string.length() < 1) {
      result = null;
    } else {
      result = string.charAt(0);
      if (string.length() > 1) {
        LOG.warn("found more than one character[" + string + "] after reading instructions from record. Will return first character[" + result + "]");
      }
    }
    return result;
  }

  public String asString(Object obj, FormatInstructions instructions) {
    return Character.toString((Character) obj);
  }


}
