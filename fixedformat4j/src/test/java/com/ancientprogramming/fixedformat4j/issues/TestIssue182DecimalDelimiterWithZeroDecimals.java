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
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Issue 182 (finding #1) - with {@code decimals = 0} there is no fraction to delimit, so
 * {@code useDecimalDelimiter = true} must not append a trailing delimiter. The stray delimiter made
 * a full-width value one character too long, and right alignment then dropped its leading digit.
 *
 * @since 2.0.0
 */
public class TestIssue182DecimalDelimiterWithZeroDecimals {

  private final FixedFormatManager manager = FixedFormatManagerImpl.create();

  @Test
  public void testExportWritesNoDelimiterWhenThereIsNoFraction() {
    assertEquals("123450012300042", manager.export(amounts(new BigDecimal("12345"), 123d, 42f)));
  }

  @Test
  public void testFullWidthValuesRoundTrip() {
    Amounts182 loaded = manager.load(Amounts182.class, manager.export(amounts(new BigDecimal("12345"), 99999d, 54321f)));

    assertEquals(new BigDecimal("12345"), loaded.getBigDecimal());
    assertEquals(99999d, loaded.getDoubleValue());
    assertEquals(54321f, loaded.getFloatValue());
  }

  @Test
  public void testDataWrittenWithTheTrailingDelimiterStillLoads() {
    Amounts182 loaded = manager.load(Amounts182.class, "1234.0123.0042.");

    assertEquals(new BigDecimal("1234"), loaded.getBigDecimal());
    assertEquals(123d, loaded.getDoubleValue());
    assertEquals(42f, loaded.getFloatValue());
  }

  private static Amounts182 amounts(BigDecimal bigDecimal, Double doubleValue, Float floatValue) {
    Amounts182 amounts = new Amounts182();
    amounts.setBigDecimal(bigDecimal);
    amounts.setDoubleValue(doubleValue);
    amounts.setFloatValue(floatValue);
    return amounts;
  }

  @Record
  public static class Amounts182 {

    private BigDecimal bigDecimal;
    private Double doubleValue;
    private Float floatValue;

    @Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 0, useDecimalDelimiter = true)
    public BigDecimal getBigDecimal() {
      return bigDecimal;
    }

    public void setBigDecimal(BigDecimal bigDecimal) {
      this.bigDecimal = bigDecimal;
    }

    @Field(offset = 6, length = 5, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 0, useDecimalDelimiter = true)
    public Double getDoubleValue() {
      return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
      this.doubleValue = doubleValue;
    }

    @Field(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 0, useDecimalDelimiter = true)
    public Float getFloatValue() {
      return floatValue;
    }

    public void setFloatValue(Float floatValue) {
      this.floatValue = floatValue;
    }
  }
}
