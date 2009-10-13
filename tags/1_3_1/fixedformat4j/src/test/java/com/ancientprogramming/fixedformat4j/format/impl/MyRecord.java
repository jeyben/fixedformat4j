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

import com.ancientprogramming.fixedformat4j.annotation.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * A record used in testcases
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
@Record
public class MyRecord {

  private String stringData;
  private Integer integerData;
  private Date dateData;
  private Character charData;
  private Boolean booleanData;
  private Long longData;
  private Double doubleData;
  private Float floatData;
  private BigDecimal bigDecimalData;
  private float simpleFloatData;


  @Field(offset = 1, length = 10, align = Align.RIGHT, paddingChar = ' ')
  public String getStringData() {
    return stringData;
  }

  public void setStringData(String stringData) {
    this.stringData = stringData;
  }

  @Field(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0')
  public Integer getIntegerData() {
    return integerData;
  }

  public void setIntegerData(Integer integerData) {
    this.integerData = integerData;
  }

  @Field(offset = 16, length = 8)
  public Date getDateData() {
    return dateData;
  }

  public void setDateData(Date dateData) {
    this.dateData = dateData;
  }

  @Field(offset = 24, length = 1)
  public Character getCharData() {
    return charData;
  }

  public void setCharData(Character charData) {
    this.charData = charData;
  }

  @Field(offset = 25, length = 1)
  public Boolean isBooleanData() {
    return booleanData;
  }

  public void setBooleanData(Boolean booleanData) {
    this.booleanData = booleanData;
  }

  @Field(offset = 26, length = 4, align = Align.RIGHT, paddingChar = '0')
  public Long getLongData() {
    return longData;
  }

  public void setLongData(Long longData) {
    this.longData = longData;
  }

  @Field(offset = 30, length = 10, align = Align.RIGHT, paddingChar = '0')
  public Double getDoubleData() {
    return doubleData;
  }

  public void setDoubleData(Double doubleData) {
    this.doubleData = doubleData;
  }

  @Field(offset = 40, length = 10, align = Align.RIGHT, paddingChar = '0')
  public Float getFloatData() {
    return floatData;
  }

  public void setFloatData(Float floatData) {
    this.floatData = floatData;
  }

  @Field(offset = 50, length = 10, align = Align.RIGHT, paddingChar = '0')
  @FixedFormatDecimal(decimals = 4, decimalDelimiter = ' ', useDecimalDelimiter = true)
  @FixedFormatNumber(sign = Sign.PREPEND)
  public BigDecimal getBigDecimalData() {
    return bigDecimalData;
  }

  public void setBigDecimalData(BigDecimal bigDecimalData) {
    this.bigDecimalData = bigDecimalData;
  }

  @Field(offset = 60, length = 10, align = Align.RIGHT, paddingChar = '0')
  public float getSimpleFloatData() {
    return simpleFloatData;
  }

  public void setSimpleFloatData(float simpleFloatData) {
    this.simpleFloatData = simpleFloatData;
  }


  @Record
  static class MyStaticNestedClass {

    private String stringData;

    @Field(offset = 1, length = 10)
    public String getStringData() {
      return stringData;
    }

    public void setStringData(String stringData) {
      this.stringData = stringData;
    }
  }

  @Record
  class MyInnerClass {

    private String stringData;

    @Field(offset = 1, length = 10)
    public String getStringData() {
      return stringData;
    }

    public void setStringData(String stringData) {
      this.stringData = stringData;
    }
  }
}