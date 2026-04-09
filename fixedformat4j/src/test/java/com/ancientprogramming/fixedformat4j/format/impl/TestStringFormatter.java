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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class TestStringFormatter {

  private FixedFormatter formatter = new StringFormatter();

  @Test
  public void testParse() {
    assertEquals("string", formatter.parse("string    ", new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null)));
    assertEquals("s", formatter.parse("s", new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null)));
    assertEquals("", formatter.parse("", new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null)));
  }

  @Test
  public void testFormat() {
    assertEquals("          ", formatter.format(null, new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null)));
    assertEquals("          ", formatter.format("", new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null)));
    assertEquals("a string i", formatter.format("a string is too long", new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null)));
  }

  @Test
  public void testRightAlign() {
    assertEquals("    hello!", formatter.format("hello!", new FormatInstructions(10, Align.RIGHT, ' ', null, null, null, null)));
    assertEquals("hello!", formatter.parse("    hello!", new FormatInstructions(10, Align.RIGHT, ' ', null, null, null, null)));
  }

  @Test
  public void testCustomPaddingChar() {
    assertEquals("hello_____", formatter.format("hello", new FormatInstructions(10, Align.LEFT, '_', null, null, null, null)));
    assertEquals("hello", formatter.parse("hello_____", new FormatInstructions(10, Align.LEFT, '_', null, null, null, null)));
  }

  @Test
  public void testStringExactlyAtFieldLength() {
    assertEquals("helloworld", formatter.format("helloworld", new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null)));
    assertEquals("helloworld", formatter.parse("helloworld", new FormatInstructions(10, Align.LEFT, ' ', null, null, null, null)));
  }
}
