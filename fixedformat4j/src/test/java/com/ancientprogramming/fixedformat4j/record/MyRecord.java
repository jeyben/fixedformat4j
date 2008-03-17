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
package com.ancientprogramming.fixedformat4j.record;

import com.ancientprogramming.fixedformat4j.annotation.*;

import java.util.Date;

/**
 * A record used in testcases
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public abstract class MyRecord implements Record {

  @FixedFormatField(offset = 1, length = 10, align = Align.RIGHT, paddingChar = ' ')
  public abstract String getStringData();

  @FixedFormatField(offset = 1, length = 10, align = Align.RIGHT, paddingChar = ' ')
  public abstract void setStringData(String stringValue);

  @FixedFormatField(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0')
  public abstract Integer getIntegerData();

  @FixedFormatField(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0')
  public abstract void setIntegerData(Integer intValue);

  @FixedFormatField(offset = 16, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  public abstract void setDateData(Date dateValue);

  @FixedFormatField(offset = 16, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  public abstract Date getDateData();

  @FixedFormatField(offset = 24, length = 1)
  public abstract void setCharacterData(Character characterValue);

  @FixedFormatField(offset = 24, length = 1)
  public abstract Character getCharacterData();

  @FixedFormatField(offset = 25, length = 1)
  @FixedFormatBoolean
  public abstract void setBooleanData(Boolean BooleanValue);

  @FixedFormatField(offset = 25, length = 1)
  @FixedFormatBoolean
  public abstract Boolean getBooleanData();


  @FixedFormatField(offset = 26, length = 4, align = Align.RIGHT, paddingChar = '0')
  public abstract Long getLongData();

  @FixedFormatField(offset = 26, length = 4, align = Align.RIGHT, paddingChar = '0')
  public abstract void setLongData(Long longValue);

  @FixedFormatField(offset = 30, length = 10, align = Align.RIGHT, paddingChar = '0')
  @FixedFormatDecimal
  public abstract Double getDoubleData();

  @FixedFormatField(offset = 30, length = 10, align = Align.RIGHT, paddingChar = '0')
  @FixedFormatDecimal
  public abstract void setDoubleData(Double longValue);

  @FixedFormatField(offset = 40, length = 10, align = Align.RIGHT, paddingChar = '0')
  @FixedFormatDecimal
  public abstract Float getFloatData();

  @FixedFormatField(offset = 40, length = 10, align = Align.RIGHT, paddingChar = '0')
  @FixedFormatDecimal
  public abstract void setFloatData(Float longValue);


}