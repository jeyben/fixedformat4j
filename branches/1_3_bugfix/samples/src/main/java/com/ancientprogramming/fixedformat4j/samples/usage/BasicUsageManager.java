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

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.samples.basic.BasicRecord;

/**
 * Shows the basic usage
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.2.0
 */
public class BasicUsageManager {

  public static void main(String[] args) {

//START-SNIPPET: basicusagemanager
    FixedFormatManager manager = new FixedFormatManagerImpl();

    //load the string into an object representation
    BasicUsageRecord record = manager.load(BasicUsageRecord.class, "initial string");

    //operate the record in object representation
    record.setStringData("some other string");

    //export back to string representation
    String exportedString = manager.export(record);
    //... do something with the new string
//END-SNIPPET: basicusagemanager
  }
}
