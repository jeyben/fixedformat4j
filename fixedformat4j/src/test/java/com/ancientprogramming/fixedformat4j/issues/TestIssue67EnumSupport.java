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

  enum Status { ACTIVE, INACTIVE, PENDING }

  enum Priority { LOW, MEDIUM, HIGH }

  // -------------------------------------------------------------------------
  // Record: LITERAL default (no annotation), space padding (default)
  // -------------------------------------------------------------------------

  @Record(length = 20)
  public static class LiteralDefaultRecord {
    private Status status;

    @Field(offset = 1, length = 10)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  // -------------------------------------------------------------------------
  // Record: LITERAL explicit annotation, space padding
  // -------------------------------------------------------------------------

  @Record(length = 20)
  public static class LiteralExplicitRecord {
    private Status status;

    @Field(offset = 1, length = 10)
    @FixedFormatEnum(EnumFormat.LITERAL)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  // -------------------------------------------------------------------------
  // Record: NUMERIC, space padding
  // -------------------------------------------------------------------------

  @Record(length = 20)
  public static class NumericRecord {
    private Status status;

    @Field(offset = 1, length = 2)
    @FixedFormatEnum(EnumFormat.NUMERIC)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  // -------------------------------------------------------------------------
  // Record: LITERAL, non-space paddingChar '*', LEFT-aligned (padding on right)
  // -------------------------------------------------------------------------

  @Record(length = 20)
  public static class StarPaddingLeftAlignRecord {
    private Status status;

    @Field(offset = 1, length = 10, paddingChar = '*', align = Align.LEFT)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  // -------------------------------------------------------------------------
  // Record: LITERAL, non-space paddingChar '*', RIGHT-aligned (padding on left)
  // -------------------------------------------------------------------------

  @Record(length = 20)
  public static class StarPaddingRightAlignRecord {
    private Status status;

    @Field(offset = 1, length = 10, paddingChar = '*', align = Align.RIGHT)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  // -------------------------------------------------------------------------
  // Record: NUMERIC, paddingChar '0', RIGHT-aligned (leading zeros)
  // -------------------------------------------------------------------------

  @Record(length = 20)
  public static class ZeroPaddingNumericRecord {
    private Priority priority;

    @Field(offset = 1, length = 3, paddingChar = '0', align = Align.RIGHT)
    @FixedFormatEnum(EnumFormat.NUMERIC)
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
  }

  // -------------------------------------------------------------------------
  // Record: validation failure — enum name too long for @Field length
  // -------------------------------------------------------------------------

  @Record(length = 10)
  public static class TooShortFieldRecord {
    private Status status;

    // Status.INACTIVE is 8 chars, but length is only 3 → should fail validation
    @Field(offset = 1, length = 3)
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
  }

  // -------------------------------------------------------------------------
  // Tests
  // -------------------------------------------------------------------------

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // --- LITERAL default ---

  @Test
  public void literalDefault_load() {
    LiteralDefaultRecord record = manager.load(LiteralDefaultRecord.class, "ACTIVE    ");
    assertEquals(Status.ACTIVE, record.getStatus());
  }

  @Test
  public void literalDefault_export() {
    LiteralDefaultRecord record = new LiteralDefaultRecord();
    record.setStatus(Status.INACTIVE);
    String exported = manager.export(record);
    assertTrue(exported.startsWith("INACTIVE"), "exported string should start with INACTIVE, got: " + exported);
  }

  @Test
  public void literalDefault_roundTrip() {
    LiteralDefaultRecord orig = new LiteralDefaultRecord();
    orig.setStatus(Status.PENDING);
    String exported = manager.export(orig);
    LiteralDefaultRecord loaded = manager.load(LiteralDefaultRecord.class, exported);
    assertEquals(Status.PENDING, loaded.getStatus());
  }

  // --- LITERAL explicit ---

  @Test
  public void literalExplicit_load() {
    LiteralExplicitRecord record = manager.load(LiteralExplicitRecord.class, "ACTIVE    ");
    assertEquals(Status.ACTIVE, record.getStatus());
  }

  @Test
  public void literalExplicit_roundTrip() {
    LiteralExplicitRecord orig = new LiteralExplicitRecord();
    orig.setStatus(Status.INACTIVE);
    String exported = manager.export(orig);
    LiteralExplicitRecord loaded = manager.load(LiteralExplicitRecord.class, exported);
    assertEquals(Status.INACTIVE, loaded.getStatus());
  }

  // --- NUMERIC ---

  @Test
  public void numeric_load_ordinal0() {
    NumericRecord record = manager.load(NumericRecord.class, "0 ");
    assertEquals(Status.ACTIVE, record.getStatus());
  }

  @Test
  public void numeric_load_ordinal1() {
    NumericRecord record = manager.load(NumericRecord.class, "1 ");
    assertEquals(Status.INACTIVE, record.getStatus());
  }

  @Test
  public void numeric_export_inactive() {
    NumericRecord record = new NumericRecord();
    record.setStatus(Status.INACTIVE);
    String exported = manager.export(record);
    assertEquals("1", exported.substring(0, 1), "INACTIVE has ordinal 1");
  }

  @Test
  public void numeric_roundTrip() {
    NumericRecord orig = new NumericRecord();
    orig.setStatus(Status.PENDING);
    String exported = manager.export(orig);
    NumericRecord loaded = manager.load(NumericRecord.class, exported);
    assertEquals(Status.PENDING, loaded.getStatus());
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
    // Align.LEFT.remove("ACTIVE****", '*') → "ACTIVE"
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
  //
  // Align.RIGHT.remove("****ACTIVE", '*') → "ACTIVE"
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
    // Align.RIGHT.remove("****ACTIVE", '*') → "ACTIVE"
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
  //
  // ordinal 1 → "1" → RIGHT.apply("1", 3, '0') → "001"
  // parsing "001" → RIGHT.remove("001", '0') → "1" → ordinal 1 → MEDIUM
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
    // "001" → strip leading '0' via RIGHT.remove → "1" → ordinal 1 → MEDIUM
    ZeroPaddingNumericRecord record = manager.load(ZeroPaddingNumericRecord.class, "001                ");
    assertEquals(Priority.MEDIUM, record.getPriority());
  }

  @Test
  public void zeroPadding_numeric_rightAlign_load_high() {
    // "002" → strip leading '0' → "2" → ordinal 2 → HIGH
    ZeroPaddingNumericRecord record = manager.load(ZeroPaddingNumericRecord.class, "002                ");
    assertEquals(Priority.HIGH, record.getPriority());
  }

  @Test
  public void zeroPadding_numeric_rightAlign_roundTrip_high() {
    ZeroPaddingNumericRecord orig = new ZeroPaddingNumericRecord();
    orig.setPriority(Priority.HIGH);  // ordinal 2 → "002" → strip → "2" → ordinal 2 → HIGH
    String exported = manager.export(orig);
    ZeroPaddingNumericRecord loaded = manager.load(ZeroPaddingNumericRecord.class, exported);
    assertEquals(Priority.HIGH, loaded.getPriority());
  }

  // -------------------------------------------------------------------------
  // Validation tests
  // -------------------------------------------------------------------------

  @Test
  public void validation_enumNameTooLongForField_throwsException() {
    // Status.INACTIVE = 8 chars, but @Field length = 3 → validation should reject
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(TooShortFieldRecord.class, "ACT"));
    assertTrue(ex.getMessage().contains("max length"), "exception should mention max length: " + ex.getMessage());
  }

  @Test
  public void invalidEnumName_throwsFixedFormatException() {
    // "UNKNOWN" is not a valid Status constant
    assertThrows(FixedFormatException.class,
        () -> manager.load(LiteralDefaultRecord.class, "UNKNOWN   "));
  }

  @Test
  public void invalidOrdinal_throwsFixedFormatException() {
    // ordinal 99 is out of range for Status (which has 3 constants)
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(NumericRecord.class, "99"));
    // The manager wraps it in ParseException; the cause carries "out of range"
    assertNotNull(ex.getCause(), "cause should be present");
    assertTrue(ex.getCause().getMessage().contains("out of range"),
        "cause should mention out of range: " + ex.getCause().getMessage());
  }
}
