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

import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

import java.math.BigDecimal;

/**
 * Formatter for {@link BigDecimal} data
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class BigDecimalFormatter extends AbstractDecimalFormatter<BigDecimal> {

    public BigDecimal asObject(String string, FormatInstructions instructions) {
      String toConvert = getStringToConvert(string, instructions);
      return new BigDecimal("".equals(toConvert) ? "0" : toConvert);
    }
}
