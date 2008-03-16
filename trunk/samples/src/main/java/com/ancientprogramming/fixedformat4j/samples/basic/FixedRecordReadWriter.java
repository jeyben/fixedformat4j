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
import com.ancientprogramming.fixedformat4j.record.Record;

import java.util.Date;

/**
 * Using property annotations we only have to define our annotations once for both reading and writing
 */
@FixedFormatRecord(length = 41)
public abstract class FixedRecordReadWriter implements Record {

  @FixedFormatField(offset = 1, length = 1)
  private Character recordType;

  @FixedFormatField(offset = 2, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  private Date date;

  @FixedFormatField(offset = 10, length = 10, paddingChar = '0', paddingDirection = Direction.LEFT)
  @FixedFormatDecimal(useDecimalDelimiter = false)
  private Double amount;

  @FixedFormatField(offset = 20, length = 20)
  private String description;

  @FixedFormatField(offset = 40, length = 1)
  @FixedFormatBoolean
  private Boolean ok;

  public Character getRecordType() {
    return recordType;
  }

  public void setRecordType(Character recordType) {
    this.recordType = recordType;
  }

  public Date getDate() {
    return date;
  }

  public void setDate(Date date) {
    this.date = date;
  }

  public Double getAmount() {
    return amount;
  }

  public void setAmount(Double amount) {
    this.amount = amount;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

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
