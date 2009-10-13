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
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Fields;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;

import java.util.Date;

/**
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
@Record
public class MultibleFieldsRecord {

  private String stringData;
  private Date dateData;
  private Integer integerdata;

  @Field(offset = 1, length = 10, align = Align.RIGHT, paddingChar = ' ')
  public String getStringData() {
    return stringData;
  }

  public void setStringData(String stringData) {
    this.stringData = stringData;
  }

  @Fields({@Field(offset = 11, length = 8), @Field(offset = 19, length = 8)})
  public Date getDateData() {
    return dateData;
  }

  public void setDateData(Date dateData) {
    this.dateData = dateData;
  }

  @Field(offset = 50, length = 4, align = Align.RIGHT, paddingChar = '0')
  public Integer getIntegerdata() {
    return integerdata;
  }

  public void setIntegerdata(Integer integerdata) {
    this.integerdata = integerdata;
  }
}
