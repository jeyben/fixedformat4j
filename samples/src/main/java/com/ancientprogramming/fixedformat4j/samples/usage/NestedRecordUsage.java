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
 * Shows how to access nested record data
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.3.0
 */
public class NestedRecordUsage {

  public static void main(String[] args) {

//START-SNIPPET: nestedrecordusage
    FixedFormatManager manager = new FixedFormatManagerImpl();

    //load the string into an object representation
    String text = "foo  bar       001232008-10-21";
    System.out.println(text);
    NestedRecord record = manager.load(NestedRecord.class, text);

    //print the output
    System.out.println(record.getStringData());
    BasicRecord nestedRecord = record.getRecord();
    System.out.println(nestedRecord.getStringData());
    System.out.println(nestedRecord.getIntegerData());
    System.out.println(nestedRecord.getDateData());

    //modify the nested record and export
    nestedRecord.setStringData("fubar");
    nestedRecord.setIntegerData(9876);

    String exportedString = manager.export(record);
    System.out.println(exportedString);
//END-SNIPPET: nestedrecordusage
  }
}