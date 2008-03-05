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
package com.ancientprogramming.fixedformat4j.format;

/**
 * Handles default formatting and parsing based on FixedFormatAnnotation values.
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public abstract class AbstractFixedFormatter implements FixedFormatter {
  public Object parse(String value, FixedFormatData data) {
    Object result = null;
    if (value != null) {
    result = asObject(data.getPaddingDirection().remove(value, data.getPaddingChar()), data);
    }
    return result;
  }

  public String format(Object value, FixedFormatData data) {
    return data.getPaddingDirection().apply(asString(value, data), data.getLength(), data.getPaddingChar());
  }

  public boolean requiresPattern() {
    return false;
  }

  public boolean requiresBoolean() {
    return false;
  }

  public boolean requiresDecimal() {
    return false;
  }

  public abstract Object asObject(String string, FixedFormatData data);

  public abstract String asString(Object obj, FixedFormatData data);
}
