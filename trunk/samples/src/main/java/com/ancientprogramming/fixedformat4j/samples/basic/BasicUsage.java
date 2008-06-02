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

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;

/**
 * Shows the basic usage
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.2.0
 */
//START-SNIPPET: basicusage
public class BasicUsage {

  private static FixedFormatManager manager = new FixedFormatManagerImpl();

  public static void main(String[] args) {
    String string = "string    001232008-05-29";
    BasicRecord record = manager.load(BasicRecord.class, string);

    System.out.println("The parsed string: " + record.getStringData());
    System.out.println("The parsed integer: " + record.getIntegerData());
    System.out.println("The parsed date: " + record.getDateData());

    record.setIntegerData(100);
    System.out.println("Exported: " + manager.export(record));
  }
}
//END-SNIPPET: basicusage
