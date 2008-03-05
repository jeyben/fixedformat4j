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

import com.ancientprogramming.fixedformat4j.annotation.Direction;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatData;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import junit.framework.TestCase;

/**
 * todo: comment needed
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class TestBooleanFormatter extends TestCase {

  FixedFormatter formatter = new BooleanFormatter();

  public void testParse() {
    assertEquals(true, formatter.parse("T", new FixedFormatData(1, Direction.RIGHT, ' ', null, new FixedFormatBooleanData("T", "F"), null)));
    assertEquals(false, formatter.parse("F ", new FixedFormatData(2, Direction.RIGHT, ' ', null, new FixedFormatBooleanData("T", "F"), null)));
    try {
      formatter.parse("", new FixedFormatData(0, Direction.RIGHT, ' ', null, new FixedFormatBooleanData("T", "F"), null));
    } catch (FixedFormatException e) {
      //expected as string is empty
    }
  }

  public void testFormat() {
    assertEquals("T", formatter.format(true, new FixedFormatData(1, Direction.RIGHT, ' ', null, new FixedFormatBooleanData("T", "F"), null)));
    assertEquals("F", formatter.format(false, new FixedFormatData(1, Direction.RIGHT, ' ', null, new FixedFormatBooleanData("T", "F"), null)));
  }
}
