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

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatter;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;

/**
 * A record used in testcases which contains annotated fields that reference instance of other annotated type.
 */
@Record
public class MyOtherRecord implements FixedFormatter {

  private MyRecord myRecord;

  public MyOtherRecord(FormatContext context) {
    // dummy
  }

  public MyOtherRecord(MyRecord myRecord) {
    this.myRecord = myRecord;
  }

  @Field(offset = 1, length = 0, formatter = MyOtherRecord.class)
  public MyRecord getStringData() {
    return myRecord;
  }

  public Object parse(String value, FormatInstructions instructions) throws FixedFormatException {
    return null;
  }

  public String format(Object value, FormatInstructions instructions) throws FixedFormatException {
    return "";
  }
}