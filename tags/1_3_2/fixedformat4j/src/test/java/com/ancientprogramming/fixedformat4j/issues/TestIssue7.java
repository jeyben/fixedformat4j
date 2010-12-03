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
 * Verifies Issue 7 - record contains other fixedformatted records
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.3.0
 */
public class TestIssue7 extends TestCase {

  FixedFormatManager fixedFormatManager = new FixedFormatManagerImpl();

  @Test
  public void testLoadedNestedRecords() {
    String text20 = "0123402345foo  bar  ";

    Issue7 issue7 = fixedFormatManager.load(Issue7.class, text20);
    assertEquals(1234, issue7.getNumber());
    assertEquals(2345, issue7.getNestedIssue7().getNumber());
    assertEquals("foo", issue7.getNestedIssue7().getString());
    assertEquals("bar", issue7.getString());
  }

  @Test
  public void testExportNestedRecords() {
    String text20 = "0123402345foo  bar  ";

    NestedIssue7 nestedIssue7 = new NestedIssue7();
    nestedIssue7.setString("foo");
    nestedIssue7.setNumber(2345);

    Issue7 issue7 = new Issue7();
    issue7.setNumber(1234);
    issue7.setNestedIssue7(nestedIssue7);
    issue7.setString("bar");

    String actual = fixedFormatManager.export(issue7);
    assertEquals(text20, actual);
  }


  @Record
  public static class Issue7 {

    private int number;
    private NestedIssue7 nestedIssue7;
    private String string;

    @Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0')
    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    @Field(offset = 6, length = 10)
       public NestedIssue7 getNestedIssue7() {
      return nestedIssue7;
    }

    public void setNestedIssue7(NestedIssue7 nestedIssue7) {
      this.nestedIssue7 = nestedIssue7;
    }

    @Field(offset = 16, length = 5)
    public String getString() {
      return string;
    }

    public void setString(String string) {
      this.string = string;
    }
  }

  @Record
  public static class NestedIssue7 {

    private int number;
    private String string;

    @Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0')
    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    @Field(offset = 6, length = 5)
    public String getString() {
      return string;
    }

    public void setString(String string) {
      this.string = string;
    }
  }
}