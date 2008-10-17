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
package com.ancientprogramming.fixedformat4j.issues;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * Verifies Issue 9
 * 
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.2.1
 */
public class TestIssue9 extends TestCase {

  FixedFormatManager fixedFormatManager = new FixedFormatManagerImpl();

  @Test
  public void testLastColumnIsIgnored() {
    String text21 = "1234567890some       ";
    String text20 = "1234567890some      ";
    String text19 = "1234567890some     ";
    String text11 = "1234567890x";
    String text10 = "1234567890";
    String text9 = "123456789";

    assertText(1234567890, "some", fixedFormatManager.load(Issue9.class, text21));
    assertText(1234567890, "some", fixedFormatManager.load(Issue9.class, text20));  
    assertText(1234567890, "some", fixedFormatManager.load(Issue9.class, text19));
    assertText(1234567890, "x", fixedFormatManager.load(Issue9.class, text11));
    assertText(1234567890, null, fixedFormatManager.load(Issue9.class, text10));
    assertText(123456789, null, fixedFormatManager.load(Issue9.class, text9));

  }

  private void assertText(int expectedNumber, String expectedString, Issue9 actual) {
    assertEquals(expectedNumber, actual.getNumber());
    assertEquals(expectedString, actual.getString());
  }


  @Record
  public static class Issue9 {

    private int number;
    private String string;



    @Field(offset = 1, length = 10, align = Align.RIGHT, paddingChar = '0')
    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }


    @Field(offset = 11, length = 10)
    public String getString() {
      return string;
    }

    public void setString(String string) {
      this.string = string;
    }
  }
}
