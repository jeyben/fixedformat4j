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
package com.ancientprogramming.fixedformat4j.samples.usage;

import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.samples.basic.BasicRecord;

import java.util.Date;


/**
 * A record containing a simple datatype as well as a nested record annotated datatype.
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.3.0
 */
//START-SNIPPET: nestedrecord
@Record
public class NestedRecord {

  private String stringData;
  private BasicRecord record; //this instance is @Record annotated

  @Field(offset = 1, length = 5)
  public String getStringData() {
    return stringData;
  }

  public void setStringData(String stringData) {
    this.stringData = stringData;
  }

  @Field(offset = 6, length = 25)
  public BasicRecord getRecord() {
    return record;
  }

  public void setRecord(BasicRecord record) {
    this.record = record;
  }
}
//END-SNIPPET: nestedrecord
