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
 * @author Jacob von Eyben - https://eybenconsult.com
 * @since 1.0.0
 */
public class TestCharacterFormatter {

  FixedFormatter formatter = new CharacterFormatter();

  @Test
  public void testParse() {
    assertEquals('J', formatter.parse("J", new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null)));
    assertEquals('J', formatter.parse("JN", new FormatInstructions(2, Align.LEFT, ' ', null, null, null, null)));
    assertNull(formatter.parse("", new FormatInstructions(0, Align.LEFT, ' ', null, null, null, null)));
  }

  @Test
  public void testFormat() {
    assertEquals("J", formatter.format('J', new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null)));
    assertEquals(" ", formatter.format(null, new FormatInstructions(1, Align.LEFT, ' ', null, null, null, null)));
  }

  @Test
  public void testWhitespaceCharRoundTrip() {
    // Use underscore as padding char so the space value is not stripped during parsing
    assertEquals(' ', formatter.parse(" ", new FormatInstructions(1, Align.LEFT, '_', null, null, null, null)));
    assertEquals(" ", formatter.format(' ', new FormatInstructions(1, Align.LEFT, '_', null, null, null, null)));
  }

  @Test
  public void testOnlyFirstCharUsedFromLongerString() {
    // When field contains several chars, only the first is returned
    assertEquals('A', formatter.parse("ABC", new FormatInstructions(3, Align.LEFT, ' ', null, null, null, null)));
  }
}
