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

import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.util.Date;

import junit.framework.TestCase;
import org.junit.Test;

/**
 * Verifies Issue 10 - parse exception contains details for better error reporting posibilities
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.3.0
 */
public class TestIssue10 extends TestCase {

  FixedFormatManager fixedFormatManager = new FixedFormatManagerImpl();

  @Test
  public void testParseExceptionContainsDetails() {

    String text = "some 20081021123f5foobar";
    try {
      fixedFormatManager.load(Issue10.class, text);
      fail("expected a parseexception");
    } catch (ParseException e) {
      assertEquals(Issue10.class, e.getAnnotatedClass());
      assertEquals("getNumber", e.getAnnotatedMethod().getName());
      assertEquals(text, e.getCompleteText());
      assertEquals("123f5", e.getFailedText());
      assertEquals(int.class, e.getFormatContext().getDataType());
      assertEquals(5, e.getFormatInstructions().getLength());
    }
  }

  @Record
  public static class Issue10 {

    private String text;
    private Date date;
    private int number;
    private String text2;

    @Field(offset = 1, length = 5)
    public String getText() {
      return text;
    }

    public void setText(String text) {
      this.text = text;
    }

    @Field(offset = 6, length = 8)
    @FixedFormatPattern("yyyyMMdd")
    public Date getDate() {
      return date;
    }

    public void setDate(Date date) {
      this.date = date;
    }

    @Field(offset = 14, length = 5, paddingChar = '0', align = Align.RIGHT)
    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }

    @Field(offset = 19, length = 5)
    public String getText2() {
      return text2;
    }

    public void setText2(String text2) {
      this.text2 = text2;
    }
  }


}
