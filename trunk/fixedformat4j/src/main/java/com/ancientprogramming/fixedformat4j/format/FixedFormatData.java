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

import com.ancientprogramming.fixedformat4j.annotation.Direction;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;

/**
 * Contains data used when parsing and formatting data in records
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class FixedFormatData {

  private int length;
  private Direction paddingDirection;
  private char paddingChar;
  private FixedFormatPatternData fixedFormatPatternData;
  private FixedFormatBooleanData fixedFormatBooleanData;
  private FixedFormatDecimalData fixedFormatDecimalData;

  public FixedFormatData(int length, Direction paddingDirection, char paddingChar, FixedFormatPatternData fixedFormatPatternData, FixedFormatBooleanData fixedFormatBooleanData, FixedFormatDecimalData fixedFormatDecimalData) {
    this.length = length;
    this.paddingDirection = paddingDirection;
    this.paddingChar = paddingChar;
    this.fixedFormatPatternData = fixedFormatPatternData;
    this.fixedFormatBooleanData = fixedFormatBooleanData;
    this.fixedFormatDecimalData = fixedFormatDecimalData;
  }

  public int getLength() {
    return length;
  }

  public Direction getPaddingDirection() {
    return paddingDirection;
  }

  public char getPaddingChar() {
    return paddingChar;
  }

  public FixedFormatPatternData getFixedFormatPatternData() {
    return fixedFormatPatternData;
  }

  public FixedFormatBooleanData getFixedFormatBooleanData() {
    return fixedFormatBooleanData;
  }

  public FixedFormatDecimalData getFixedFormatDecimalData() {
    return fixedFormatDecimalData;
  }


  public String toString() {
    return "FixedFormatData{" +
        "length=" + length +
        ", paddingDirection=" + paddingDirection +
        ", paddingChar=" + paddingChar +
        ", fixedFormatPatternData=" + fixedFormatPatternData +
        ", fixedFormatBooleanData=" + fixedFormatBooleanData +
        ", fixedFormatDecimalData=" + fixedFormatDecimalData +
        '}';
  }
}
