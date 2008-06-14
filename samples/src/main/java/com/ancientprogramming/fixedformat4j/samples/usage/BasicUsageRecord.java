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

import java.util.Date;

/**
 * A record containing some simple datatypes to show basic parsing and formatting.
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.2.0
 */
//START-SNIPPET: basicusage
@Record
public class BasicUsageRecord {

  private String stringData;

  @Field(offset = 1, length = 35)
  public String getStringData() {
    return stringData;
  }

  public void setStringData(String stringData) {
    this.stringData = stringData;
  }
}
//END-SNIPPET: basicusage
