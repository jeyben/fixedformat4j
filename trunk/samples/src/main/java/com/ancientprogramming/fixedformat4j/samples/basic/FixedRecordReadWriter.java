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
package com.ancientprogramming.fixedformat4j.samples.basic;

import com.ancientprogramming.fixedformat4j.annotation.*;

import java.util.Date;

/**
 * Using property annotations we only have to define our annotations once for both reading and writing
 */
@Record(length = 41)
public abstract class FixedRecordReadWriter {

  private Character recordType;

  private Date date;

  private Double amount;

  private String description;

  private Boolean ok;


  @FixedFormatField(offset = 1, length = 1)
  public Character getRecordType() {
    return recordType;
  }


  public void setRecordType(Character recordType) {
    this.recordType = recordType;
  }

  @FixedFormatField(offset = 2, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  @FixedFormatField(offset = 10, length = 10, paddingChar = '0', align = Align.RIGHT)
  @FixedFormatDecimal(useDecimalDelimiter = false)
  public Double getAmount() {
    return amount;
  }

  public void setAmount(Double amount) {
    this.amount = amount;
  }

  @FixedFormatField(offset = 20, length = 20)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @FixedFormatField(offset = 40, length = 1)
  @FixedFormatBoolean
  public Boolean getOk() {
    return ok;
  }

  public void setOk(Boolean ok) {
    this.ok = ok;
  }


  public String toString() {
    return "FixedRecordReadWriter{" +
        "recordType=" + getRecordType() +
        ", date=" + getDate() +
        ", amount=" + getAmount() +
        ", description='" + getDescription() + '\'' +
        ", ok=" + getOk() +
        '}';
  }
}
