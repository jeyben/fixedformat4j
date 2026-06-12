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
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Issue 130 &mdash; {@code @Field.nullValue} declares a literal
 * mixed-character sentinel string that represents {@code null}, complementing
 * the uniform single-character {@code nullChar} convention.
 *
 * <p>The motivating data shape is a 4-char implied-decimal column where
 * {@code "0000"} means {@code 0}, {@code "9998"} means {@code null} and
 * {@code "0501"} means {@code 50.1}.
 *
 * @since 1.9.0
 */
public class TestIssue130NullValue {

  private final FixedFormatManager manager = FixedFormatManagerImpl.create();

  // ---------------------------------------------------------------------------
  // Load - literal sentinel slice yields null
  // ---------------------------------------------------------------------------

  @Test
  public void loadSentinelSliceOnDecimalField_returnsNull() {
    ImpliedDecimalRecord loaded = manager.load(ImpliedDecimalRecord.class, "9998");
    assertNull(loaded.getRate());
  }

  // ---------------------------------------------------------------------------
  // Load - non-sentinel slices are parsed by the formatter as usual
  // ---------------------------------------------------------------------------

  @Test
  public void loadAllZerosSlice_parsesAsZero() {
    ImpliedDecimalRecord loaded = manager.load(ImpliedDecimalRecord.class, "0000");
    assertEquals(0, BigDecimal.ZERO.compareTo(loaded.getRate()));
  }

  @Test
  public void loadRegularSlice_parsesWithImpliedDecimal() {
    ImpliedDecimalRecord loaded = manager.load(ImpliedDecimalRecord.class, "0501");
    assertEquals(new BigDecimal("50.1"), loaded.getRate());
  }

  // ---------------------------------------------------------------------------
  // Export - null emits the sentinel verbatim, non-null uses the formatter
  // ---------------------------------------------------------------------------

  @Test
  public void exportNullValue_emitsSentinelVerbatim() {
    ImpliedDecimalRecord record = new ImpliedDecimalRecord();
    record.setRate(null);
    assertEquals("9998", manager.export(record));
  }

  @Test
  public void exportRegularValue_usesFormatter() {
    ImpliedDecimalRecord record = new ImpliedDecimalRecord();
    record.setRate(new BigDecimal("50.1"));
    assertEquals("0501", manager.export(record));
  }

  // ---------------------------------------------------------------------------
  // Round-trip fidelity
  // ---------------------------------------------------------------------------

  @Test
  public void roundTripNullValue_isPreserved() {
    ImpliedDecimalRecord record = new ImpliedDecimalRecord();
    record.setRate(null);
    ImpliedDecimalRecord reloaded = manager.load(ImpliedDecimalRecord.class, manager.export(record));
    assertNull(reloaded.getRate());
  }

  @Test
  public void roundTripRegularValue_isPreserved() {
    ImpliedDecimalRecord record = new ImpliedDecimalRecord();
    record.setRate(new BigDecimal("50.1"));
    ImpliedDecimalRecord reloaded = manager.load(ImpliedDecimalRecord.class, manager.export(record));
    assertEquals(new BigDecimal("50.1"), reloaded.getRate());
  }

  // ---------------------------------------------------------------------------
  // Repeating fields - the sentinel is applied per element, like nullChar
  // ---------------------------------------------------------------------------

  @Test
  public void loadRepeatingField_sentinelElementLoadsAsNull() {
    RepeatingRecord loaded = manager.load(RepeatingRecord.class, "000199980003");
    assertEquals(Arrays.asList(1, null, 3), loaded.getAmounts());
  }

  @Test
  public void exportRepeatingField_nullElementEmitsSentinel() {
    RepeatingRecord record = new RepeatingRecord();
    record.setAmounts(Arrays.asList(1, null, 3));
    assertEquals("000199980003", manager.export(record));
  }

  // ---------------------------------------------------------------------------
  // Validation - misconfigured nullValue is rejected with a clear exception
  // ---------------------------------------------------------------------------

  @Test
  public void nullValueLengthMismatch_isRejected() {
    FixedFormatException thrown = assertThrows(FixedFormatException.class,
        () -> manager.load(LengthMismatchRecord.class, "9998"));
    assertTrue(thrown.getMessage().contains("getValue"), thrown.getMessage());
  }

  @Test
  public void nullValueOnRestOfLineField_isRejected() {
    FixedFormatException thrown = assertThrows(FixedFormatException.class,
        () -> manager.load(RestOfLineRecord.class, "data"));
    assertTrue(thrown.getMessage().contains("getRemainder"), thrown.getMessage());
  }

  @Test
  public void nullValueOnPrimitiveField_isRejected() {
    FixedFormatException thrown = assertThrows(FixedFormatException.class,
        () -> manager.load(PrimitiveRecord.class, "9998"));
    assertTrue(thrown.getMessage().contains("getAmount"), thrown.getMessage());
  }

  @Test
  public void nullValueCombinedWithNullChar_isRejected() {
    FixedFormatException thrown = assertThrows(FixedFormatException.class,
        () -> manager.load(BothSentinelsRecord.class, "9998"));
    assertTrue(thrown.getMessage().contains("getValue"), thrown.getMessage());
  }

  @Record(length = 4, paddingChar = '0')
  public static class ImpliedDecimalRecord {
    private BigDecimal rate;

    @Field(offset = 1, length = 4, align = Align.RIGHT, paddingChar = '0', nullValue = "9998")
    @FixedFormatDecimal(decimals = 1)
    public BigDecimal getRate() {
      return rate;
    }

    public void setRate(BigDecimal rate) {
      this.rate = rate;
    }
  }

  @Record(length = 12, paddingChar = '0')
  public static class RepeatingRecord {
    private List<Integer> amounts;

    @Field(offset = 1, length = 4, count = 3, align = Align.RIGHT, paddingChar = '0', nullValue = "9998")
    public List<Integer> getAmounts() {
      return amounts;
    }

    public void setAmounts(List<Integer> amounts) {
      this.amounts = amounts;
    }
  }

  @Record(length = 4)
  public static class LengthMismatchRecord {
    private String value;

    @Field(offset = 1, length = 4, nullValue = "99")
    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @Record
  public static class RestOfLineRecord {
    private String remainder;

    @Field(offset = 1, length = Field.REST_OF_LINE, nullValue = "9998")
    public String getRemainder() {
      return remainder;
    }

    public void setRemainder(String remainder) {
      this.remainder = remainder;
    }
  }

  @Record(length = 4)
  public static class PrimitiveRecord {
    private int amount;

    @Field(offset = 1, length = 4, nullValue = "9998")
    public int getAmount() {
      return amount;
    }

    public void setAmount(int amount) {
      this.amount = amount;
    }
  }

  @Record(length = 4)
  public static class BothSentinelsRecord {
    private String value;

    @Field(offset = 1, length = 4, nullChar = ' ', nullValue = "9998")
    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
