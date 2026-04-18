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
package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that {@code nullChar} on a {@code @Field} with a primitive return type is rejected
 * at validation time. Primitives can never be {@code null}, so the configuration is always
 * invalid regardless of whether null-awareness would otherwise be active.
 *
 * @since 1.7.1
 */
public class TestNullCharPrimitiveValidation {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // ---------------------------------------------------------------------------
  // Rejected: primitive field types
  // ---------------------------------------------------------------------------

  @Test
  public void load_nullCharOnIntField_throwsFixedFormatException() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(IntPrimitiveRecord.class, "     "));
    assertTrue(ex.getMessage().contains("getValue"),
        "Exception message should mention the getter: " + ex.getMessage());
  }

  @Test
  public void export_nullCharOnIntField_throwsFixedFormatException() {
    IntPrimitiveRecord record = new IntPrimitiveRecord();
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.export(record));
    assertTrue(ex.getMessage().contains("getValue"),
        "Exception message should mention the getter: " + ex.getMessage());
  }

  @Test
  public void load_nullCharOnLongField_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(LongPrimitiveRecord.class, "     "));
  }

  @Test
  public void load_nullCharOnPrimitiveIntArray_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(IntArrayRecord.class, "          "));
  }

  // ---------------------------------------------------------------------------
  // Permitted: boxed / reference field types
  // ---------------------------------------------------------------------------

  @Test
  public void load_nullCharOnIntegerField_doesNotThrow() {
    assertDoesNotThrow(() -> manager.load(IntegerBoxedRecord.class, "     "));
  }

  @Test
  public void load_nullCharOnIntegerArray_doesNotThrow() {
    assertDoesNotThrow(() -> manager.load(IntegerArrayRecord.class, "          "));
  }

  // ---------------------------------------------------------------------------
  // Permitted: no nullChar set on primitive (default UNSET_NULL_CHAR)
  // ---------------------------------------------------------------------------

  @Test
  public void load_noNullCharOnIntField_doesNotThrow() {
    assertDoesNotThrow(() -> manager.load(IntPrimitiveNoNullCharRecord.class, "00042"));
  }

  // ---------------------------------------------------------------------------
  // Record definitions
  // ---------------------------------------------------------------------------

  @Record(length = 5)
  public static class IntPrimitiveRecord {
    private int value;

    @Field(offset = 1, length = 5, paddingChar = '0', nullChar = ' ')
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
  }

  @Record(length = 5)
  public static class LongPrimitiveRecord {
    private long value;

    @Field(offset = 1, length = 5, paddingChar = '0', nullChar = ' ')
    public long getValue() { return value; }
    public void setValue(long value) { this.value = value; }
  }

  @Record(length = 10)
  public static class IntArrayRecord {
    private int[] values;

    @Field(offset = 1, length = 5, count = 2, paddingChar = '0', nullChar = ' ')
    public int[] getValues() { return values; }
    public void setValues(int[] values) { this.values = values; }
  }

  @Record(length = 5)
  public static class IntegerBoxedRecord {
    private Integer value;

    @Field(offset = 1, length = 5, paddingChar = '0', nullChar = ' ')
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
  }

  @Record(length = 10)
  public static class IntegerArrayRecord {
    private Integer[] values;

    @Field(offset = 1, length = 5, count = 2, paddingChar = '0', nullChar = ' ')
    public Integer[] getValues() { return values; }
    public void setValues(Integer[] values) { this.values = values; }
  }

  @Record(length = 5)
  public static class IntPrimitiveNoNullCharRecord {
    private int value;

    @Field(offset = 1, length = 5, paddingChar = '0')
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
  }
}
