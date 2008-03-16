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

import com.ancientprogramming.fixedformat4j.record.RecordFactory;

/**
 * This sample demonstrates the basics of the fixedformat4j api
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class BasicReadWriteSampleUsingPropertyAnnotations {

  /**
   * This text represents the following data
   * <pre>
   * A           = could be a record type
   * 20080316    = the date 2008-03-16
   * 0000140050  = the amount 1400.50
   * A longer... = A longer description
   * T           = boolean true value
   * </pre>
   */
  public static final String FIXED_RECORD = "A200803160000140050A longer desciption T";

  public static void main(String[] args) {
    FixedRecordReadWriter record = (FixedRecordReadWriter) RecordFactory.createInstance(FixedRecordReadWriter.class, FIXED_RECORD);

    String toString = record.toString();
    String export = record.export();

    System.out.println("export = " + export);
    System.out.println("toString = " + toString);
  }
}
