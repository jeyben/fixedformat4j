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
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies Issue 84 &mdash; a fully-padded slice loads as {@code null} when
 * {@code nullChar == paddingChar} (the "blank-is-null" convention).
 *
 * <p>The activation gate for {@code nullChar} is relaxed so that setting
 * {@code nullChar} to the same character as {@code paddingChar} enables
 * null-aware handling. The idiomatic use cases are all-spaces dates and
 * all-zeros numerics.
 *
 * @since 1.7.2
 */
public class TestIssue84BlankIsNull {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // ---------------------------------------------------------------------------
  // Space-padded reference type -> all-spaces is null
  // ---------------------------------------------------------------------------

  @Test
  public void loadAllSpacesOnStringField_returnsNull() {
    SpacePaddedStringRecord loaded = manager.load(SpacePaddedStringRecord.class, "        ");
    assertNull(loaded.getDescription());
  }

  @Test
  public void exportNullOnStringField_emitsAllSpaces() {
    SpacePaddedStringRecord record = new SpacePaddedStringRecord();
    record.setDescription(null);
    assertEquals("        ", manager.export(record));
  }

  @Test
  public void roundTripNonNullValueOnStringField_isPreserved() {
    SpacePaddedStringRecord record = new SpacePaddedStringRecord();
    record.setDescription("ABC");
    String exported = manager.export(record);
    assertEquals("ABC     ", exported);
    SpacePaddedStringRecord reloaded = manager.load(SpacePaddedStringRecord.class, exported);
    assertEquals("ABC", reloaded.getDescription());
  }

  // ---------------------------------------------------------------------------
  // Zero-padded numeric -> all-zeros is null
  // ---------------------------------------------------------------------------

  @Test
  public void loadAllZerosOnIntegerField_returnsNull() {
    ZeroPaddedIntegerRecord loaded = manager.load(ZeroPaddedIntegerRecord.class, "00000");
    assertNull(loaded.getQuantity());
  }

  @Test
  public void loadNonZeroOnIntegerField_returnsValue() {
    ZeroPaddedIntegerRecord loaded = manager.load(ZeroPaddedIntegerRecord.class, "00042");
    assertEquals(Integer.valueOf(42), loaded.getQuantity());
  }

  @Test
  public void exportNullOnIntegerField_emitsAllZeros() {
    ZeroPaddedIntegerRecord record = new ZeroPaddedIntegerRecord();
    record.setQuantity(null);
    assertEquals("00000", manager.export(record));
  }

  @Test
  public void exportValueOnIntegerField_emitsZeroPaddedDigits() {
    ZeroPaddedIntegerRecord record = new ZeroPaddedIntegerRecord();
    record.setQuantity(42);
    assertEquals("00042", manager.export(record));
  }

  // ---------------------------------------------------------------------------
  // POJO field default preserved when null-slice detected
  // ---------------------------------------------------------------------------

  @Test
  public void loadAllZerosOnRecordWithPojoDefault_preservesDefault() {
    PojoDefaultRecord loaded = manager.load(PojoDefaultRecord.class, "00000");
    assertEquals(Integer.valueOf(99), loaded.getQuantity());
  }

  // ---------------------------------------------------------------------------
  // Repeating field per-element detection with nullChar == paddingChar
  // ---------------------------------------------------------------------------

  @Test
  public void loadRepeatingStringField_blankElementBecomesNull() {
    RepeatingSpacePaddedRecord loaded = manager.load(RepeatingSpacePaddedRecord.class, "ABC   XYZ");
    List<String> codes = loaded.getCodes();
    assertEquals(3, codes.size());
    assertEquals("ABC", codes.get(0));
    assertNull(codes.get(1));
    assertEquals("XYZ", codes.get(2));
  }

  @Test
  public void exportRepeatingStringFieldWithNullElement_emitsAllSpacesForNull() {
    RepeatingSpacePaddedRecord record = new RepeatingSpacePaddedRecord();
    record.setCodes(new ArrayList<>(Arrays.asList("ABC", null, "XYZ")));
    assertEquals("ABC   XYZ", manager.export(record));
  }

  // ---------------------------------------------------------------------------
  // Primitive validation still rejects when nullChar == paddingChar
  // ---------------------------------------------------------------------------

  @Test
  public void load_blankIsNullOnPrimitive_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(PrimitiveBlankIsNullRecord.class, "     "));
  }

  // ---------------------------------------------------------------------------
  // Record definitions
  // ---------------------------------------------------------------------------

  @Record(length = 8)
  public static class SpacePaddedStringRecord {

    private String description;

    @Field(offset = 1, length = 8, paddingChar = ' ', nullChar = ' ')
    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }
  }

  @Record(length = 5)
  public static class ZeroPaddedIntegerRecord {

    private Integer quantity;

    @Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0', nullChar = '0')
    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }

  @Record(length = 5)
  public static class PojoDefaultRecord {

    private Integer quantity = 99;

    @Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0', nullChar = '0')
    public Integer getQuantity() {
      return quantity;
    }

    public void setQuantity(Integer quantity) {
      this.quantity = quantity;
    }
  }

  @Record(length = 9)
  public static class RepeatingSpacePaddedRecord {

    private List<String> codes;

    @Field(offset = 1, length = 3, count = 3, paddingChar = ' ', nullChar = ' ')
    public List<String> getCodes() {
      return codes;
    }

    public void setCodes(List<String> codes) {
      this.codes = codes;
    }
  }

  @Record(length = 5)
  public static class PrimitiveBlankIsNullRecord {

    private int hours;

    @Field(offset = 1, length = 5, paddingChar = ' ', nullChar = ' ')
    public int getHours() {
      return hours;
    }

    public void setHours(int hours) {
      this.hours = hours;
    }
  }
}
