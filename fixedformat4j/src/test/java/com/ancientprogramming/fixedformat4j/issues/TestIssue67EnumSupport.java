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
import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatEnum;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Issue 67 — enum support in fixed-format records.
 *
 * <p>Includes tests for non-space padding characters to verify that
 * {@code EnumFormatter} relies on the base-class {@code stripPadding()} mechanism
 * (which uses the field's actual {@code paddingChar}) rather than calling
 * {@code String.trim()} (which only strips spaces).
 */
public class TestIssue67EnumSupport {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // -------------------------------------------------------------------------
  // Test enums
  // -------------------------------------------------------------------------

  enum Status { ACTIVE, INACTIVE, PENDING }

  enum Priority { LOW, MEDIUM, HIGH }

  enum TooLongForField { VERY_LONG_ENUM_VALUE_NAME }

  // -------------------------------------------------------------------------
  // Test records
  // -------------------------------------------------------------------------

  @Record(length = 20)
  public static class LiteralDefaultRecord {
    private Status status;

    @Field(offset = 1, length = 10)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  @Record(length = 20)
  public static class LiteralExplicitRecord {
    private Status status;

    @Field(offset = 1, length = 10)
    @FixedFormatEnum(EnumFormat.LITERAL)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  @Record(length = 10)
  public static class NumericRecord {
    private Priority priority;

    @Field(offset = 1, length = 2)
    @FixedFormatEnum(EnumFormat.NUMERIC)
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
  }

  @Record(length = 20)
  public static class TwoFieldRecord {
    private Status status;
    private Priority priority;

    @Field(offset = 1, length = 10)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    @Field(offset = 11, length = 2)
    @FixedFormatEnum(EnumFormat.NUMERIC)
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
  }

  @Record(length = 20)
  public static class StarPaddingLeftAlignRecord {
    private Status status;

    @Field(offset = 1, length = 10, paddingChar = '*', align = Align.LEFT)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  @Record(length = 20)
  public static class StarPaddingRightAlignRecord {
    private Status status;

    @Field(offset = 1, length = 10, paddingChar = '*', align = Align.RIGHT)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  @Record(length = 20)
  public static class ZeroPaddingNumericRecord {
    private Priority priority;

    @Field(offset = 1, length = 3, paddingChar = '0', align = Align.RIGHT)
    @FixedFormatEnum(EnumFormat.NUMERIC)
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
  }

  @Record(length = 10)
  public static class TooShortFieldRecord {
    private Status status;

    @Field(offset = 1, length = 3)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  @Record(length = 5)
  public static class TooShortRecord {
    private TooLongForField value;

    @Field(offset = 1, length = 5)
    public TooLongForField getValue() { return value; }
    public void setValue(TooLongForField v) { this.value = v; }
  }

  // -------------------------------------------------------------------------
  // LITERAL (default) tests
  // -------------------------------------------------------------------------

  @Test
  public void loadLiteralDefaultNoAnnotation() {
    LiteralDefaultRecord record = manager.load(LiteralDefaultRecord.class, "ACTIVE    ");
    assertEquals(Status.ACTIVE, record.getStatus());
  }

  @Test
  public void exportLiteralDefaultNoAnnotation() {
    LiteralDefaultRecord record = new LiteralDefaultRecord();
    record.setStatus(Status.PENDING);
    String exported = manager.export(record);
    assertEquals("PENDING   " + "          ", exported);
  }

  @Test
  public void loadLiteralExplicitAnnotation() {
    LiteralExplicitRecord record = manager.load(LiteralExplicitRecord.class, "INACTIVE  ");
    assertEquals(Status.INACTIVE, record.getStatus());
  }

  @Test
  public void literalRoundTrip() {
    LiteralDefaultRecord record = new LiteralDefaultRecord();
    record.setStatus(Status.INACTIVE);
    String exported = manager.export(record);
    LiteralDefaultRecord loaded = manager.load(LiteralDefaultRecord.class, exported);
    assertEquals(Status.INACTIVE, loaded.getStatus());
  }

  @Test
  public void nullEnumFieldRoundTrip() {
    LiteralDefaultRecord record = new LiteralDefaultRecord();
    record.setStatus(null);
    String exported = manager.export(record);
    LiteralDefaultRecord loaded = manager.load(LiteralDefaultRecord.class, exported);
    assertNull(loaded.getStatus(), "null enum should round-trip as null");
  }

  // -------------------------------------------------------------------------
  // NUMERIC tests
  // -------------------------------------------------------------------------

  @Test
  public void loadNumeric() {
    NumericRecord record = manager.load(NumericRecord.class, "1         ");
    assertEquals(Priority.MEDIUM, record.getPriority());
  }

  @Test
  public void exportNumeric() {
    NumericRecord record = new NumericRecord();
    record.setPriority(Priority.HIGH);
    String exported = manager.export(record);
    assertTrue(exported.startsWith("2"), "Expected ordinal '2' for HIGH, got: " + exported);
  }

  @Test
  public void numericRoundTrip() {
    NumericRecord record = new NumericRecord();
    record.setPriority(Priority.LOW);
    String exported = manager.export(record);
    NumericRecord loaded = manager.load(NumericRecord.class, exported);
    assertEquals(Priority.LOW, loaded.getPriority());
  }

  @Test
  public void twoFieldRecordRoundTrip() {
    TwoFieldRecord record = new TwoFieldRecord();
    record.setStatus(Status.ACTIVE);
    record.setPriority(Priority.HIGH);
    String exported = manager.export(record);
    TwoFieldRecord loaded = manager.load(TwoFieldRecord.class, exported);
    assertEquals(Status.ACTIVE, loaded.getStatus());
    assertEquals(Priority.HIGH, loaded.getPriority());
  }

  // -------------------------------------------------------------------------
  // Non-space paddingChar: '*', LEFT-aligned (padding appended on the right)
  //
  // AbstractFixedFormatter.parse() calls stripPadding() which uses
  // Align.LEFT.remove(value, '*') — strips trailing '*' chars — before
  // passing the clean value to asObject(). This is why asObject() must NOT
  // call value.trim() (only strips spaces).
  // -------------------------------------------------------------------------

  @Test
  public void starPadding_leftAlign_export() {
    StarPaddingLeftAlignRecord record = new StarPaddingLeftAlignRecord();
    record.setStatus(Status.ACTIVE);
    String exported = manager.export(record);
    assertEquals("ACTIVE****", exported.substring(0, 10), "LEFT-aligned with '*' padding should be right-padded");
  }

  @Test
  public void starPadding_leftAlign_load() {
    StarPaddingLeftAlignRecord record = manager.load(StarPaddingLeftAlignRecord.class, "ACTIVE****          ");
    assertEquals(Status.ACTIVE, record.getStatus());
  }

  @Test
  public void starPadding_leftAlign_roundTrip_active() {
    StarPaddingLeftAlignRecord orig = new StarPaddingLeftAlignRecord();
    orig.setStatus(Status.ACTIVE);
    String exported = manager.export(orig);
    StarPaddingLeftAlignRecord loaded = manager.load(StarPaddingLeftAlignRecord.class, exported);
    assertEquals(Status.ACTIVE, loaded.getStatus());
  }

  @Test
  public void starPadding_leftAlign_roundTrip_inactive() {
    StarPaddingLeftAlignRecord orig = new StarPaddingLeftAlignRecord();
    orig.setStatus(Status.INACTIVE);
    String exported = manager.export(orig);
    StarPaddingLeftAlignRecord loaded = manager.load(StarPaddingLeftAlignRecord.class, exported);
    assertEquals(Status.INACTIVE, loaded.getStatus());
  }

  // -------------------------------------------------------------------------
  // Non-space paddingChar: '*', RIGHT-aligned (padding prepended on the left)
  // -------------------------------------------------------------------------

  @Test
  public void starPadding_rightAlign_export() {
    StarPaddingRightAlignRecord record = new StarPaddingRightAlignRecord();
    record.setStatus(Status.ACTIVE);
    String exported = manager.export(record);
    assertEquals("****ACTIVE", exported.substring(0, 10), "RIGHT-aligned with '*' padding should be left-padded");
  }

  @Test
  public void starPadding_rightAlign_load() {
    StarPaddingRightAlignRecord record = manager.load(StarPaddingRightAlignRecord.class, "****ACTIVE          ");
    assertEquals(Status.ACTIVE, record.getStatus());
  }

  @Test
  public void starPadding_rightAlign_roundTrip_pending() {
    StarPaddingRightAlignRecord orig = new StarPaddingRightAlignRecord();
    orig.setStatus(Status.PENDING);
    String exported = manager.export(orig);
    StarPaddingRightAlignRecord loaded = manager.load(StarPaddingRightAlignRecord.class, exported);
    assertEquals(Status.PENDING, loaded.getStatus());
  }

  // -------------------------------------------------------------------------
  // Non-space paddingChar: '0', NUMERIC mode, RIGHT-aligned (leading zeros)
  // -------------------------------------------------------------------------

  @Test
  public void zeroPadding_numeric_rightAlign_export_high() {
    ZeroPaddingNumericRecord record = new ZeroPaddingNumericRecord();
    record.setPriority(Priority.HIGH);  // ordinal 2
    String exported = manager.export(record);
    assertEquals("002", exported.substring(0, 3), "HIGH (ordinal 2) should be exported as '002'");
  }

  @Test
  public void zeroPadding_numeric_rightAlign_load_medium() {
    ZeroPaddingNumericRecord record = manager.load(ZeroPaddingNumericRecord.class, "001                ");
    assertEquals(Priority.MEDIUM, record.getPriority());
  }

  @Test
  public void zeroPadding_numeric_rightAlign_load_high() {
    ZeroPaddingNumericRecord record = manager.load(ZeroPaddingNumericRecord.class, "002                ");
    assertEquals(Priority.HIGH, record.getPriority());
  }

  @Test
  public void zeroPadding_numeric_rightAlign_roundTrip_high() {
    ZeroPaddingNumericRecord orig = new ZeroPaddingNumericRecord();
    orig.setPriority(Priority.HIGH);
    String exported = manager.export(orig);
    ZeroPaddingNumericRecord loaded = manager.load(ZeroPaddingNumericRecord.class, exported);
    assertEquals(Priority.HIGH, loaded.getPriority());
  }

  // -------------------------------------------------------------------------
  // Validation tests
  // -------------------------------------------------------------------------

  @Test
  public void enumNameExceedsFieldLengthThrowsOnLoad() {
    assertThrows(FixedFormatException.class, () ->
        manager.load(TooShortRecord.class, "     "));
  }

  @Test
  public void validation_enumNameTooLongForField_throwsException() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(TooShortFieldRecord.class, "ACT"));
    assertTrue(ex.getMessage().contains("max length"), "exception should mention max length: " + ex.getMessage());
  }

  @Test
  public void invalidLiteralNameThrowsOnParse() {
    assertThrows(Exception.class, () ->
        manager.load(LiteralDefaultRecord.class, "NOSUCHVAL "));
  }

  @Test
  public void invalidOrdinalThrowsOnParse() {
    assertThrows(Exception.class, () ->
        manager.load(NumericRecord.class, "9         "));
  }

  @Test
  public void invalidOrdinalNonNumericThrowsOnParse() {
    assertThrows(Exception.class, () ->
        manager.load(NumericRecord.class, "X         "));
  }

  @Test
  public void invalidOrdinal_throwsFixedFormatException() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(NumericRecord.class, "99"));
    assertNotNull(ex.getCause(), "cause should be present");
    assertTrue(ex.getCause().getMessage().contains("out of range"),
        "cause should mention out of range: " + ex.getCause().getMessage());
  }
}
