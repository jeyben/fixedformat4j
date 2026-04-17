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
 * Regression tests for Issue 67 - Support for Enums.
 */
public class TestIssue67EnumSupport {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // -------------------------------------------------------------------------
  // Test enums
  // -------------------------------------------------------------------------

  public enum Status {
    ACTIVE, PENDING, INACTIVE
  }

  public enum Priority {
    LOW, MEDIUM, HIGH
  }

  public enum TooLongForField {
    VERY_LONG_ENUM_VALUE_NAME
  }

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
  // Validation tests
  // -------------------------------------------------------------------------

  @Test
  public void enumNameExceedsFieldLengthThrowsOnLoad() {
    assertThrows(FixedFormatException.class, () ->
        manager.load(TooShortRecord.class, "     "));
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
}
