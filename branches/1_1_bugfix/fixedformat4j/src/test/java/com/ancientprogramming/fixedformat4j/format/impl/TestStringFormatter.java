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

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestStringFormatter extends TestCase {

  private FixedFormatter formatter = new StringFormatter();

  public void testParse() {
    FormatInstructions instructions = new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null);
    String input = "string";
    Object parseResult = formatter.parse(input, instructions);
    Assert.assertEquals(String.class, parseResult.getClass());
  }

  public void testFormat() {
    FormatInstructions instructions = new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null);
    String input = "string";
    String expected = "string    ";

    String formatResult = formatter.format(input, instructions);
    Assert.assertEquals("expected[" + expected + "] - actual[" + formatResult + "]", expected, formatResult);
  }
}
