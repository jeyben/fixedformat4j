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
import com.ancientprogramming.fixedformat4j.annotation.Fields;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.annotation.Sign;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies field-level annotation support: @Field and @Fields placed on Java fields
 * instead of getter methods. Works with both Lombok-generated and explicit getters.
 *
 * @since 1.5.0
 */
public class TestLombokFieldAnnotations {

  // Fixed test string layout (1-based):
  //   1-10  : "Jacob     "   name
  //   11-15 : "00042"        age (right-aligned, zero-padded)
  //   16-23 : "19850315"     birthDate (yyyyMMdd)
  //   24    : "Y"            active
  //   25-34 : "0000123456"   salary = 1234.56 (2 implicit decimals, no sign)
  private static final String TEST_DATA = "Jacob     0004219850315Y0000123456";

  private FixedFormatManager manager;

  @BeforeEach
  void setUp() {
    manager = new FixedFormatManagerImpl();
  }

  // ── Sunshine cases ───────────────────────────────────────────────────────────

  @Test
  void testLombokLoad_allFieldsPopulated() {
    LombokRecord record = manager.load(LombokRecord.class, TEST_DATA);

    assertEquals("Jacob", record.getName());
    assertEquals(42, record.getAge());
    assertEquals(new BigDecimal("1234.56"), record.getSalary());
    assertTrue(record.getActive());
  }

  @Test
  void testLombokExport_producesCorrectString() {
    LombokRecord record = buildLombokRecord();

    String exported = manager.export(record);

    assertEquals(TEST_DATA, exported);
  }

  @Test
  void testLombokRoundTrip_loadThenExportMatchesOriginal() {
    LombokRecord loaded = manager.load(LombokRecord.class, TEST_DATA);
    String exported = manager.export(loaded);

    assertEquals(TEST_DATA, exported);
  }

  @Test
  void testFixedFormatPattern_dateFieldParsedCorrectly() throws Exception {
    LombokRecord record = manager.load(LombokRecord.class, TEST_DATA);

    Date expected = new SimpleDateFormat("yyyyMMdd").parse("19850315");
    assertEquals(expected, record.getBirthDate());
  }

  @Test
  void testFixedFormatPattern_dateFieldExportedCorrectly() throws Exception {
    LombokRecord record = buildLombokRecord();
    String exported = manager.export(record);

    assertEquals("19850315", exported.substring(15, 23));
  }

  @Test
  void testFixedFormatBoolean_trueParsedFromY() {
    LombokRecord record = manager.load(LombokRecord.class, TEST_DATA);
    assertTrue(record.getActive());
  }

  @Test
  void testFixedFormatBoolean_falseExportedAsN() {
    LombokRecord record = buildLombokRecord();
    record.setActive(false);
    String exported = manager.export(record);

    assertEquals("N", exported.substring(23, 24));
  }

  @Test
  void testFixedFormatDecimal_bigDecimalParsedCorrectly() {
    LombokRecord record = manager.load(LombokRecord.class, TEST_DATA);
    assertEquals(new BigDecimal("1234.56"), record.getSalary());
  }

  @Test
  void testFixedFormatDecimal_bigDecimalExportedCorrectly() {
    LombokRecord record = buildLombokRecord();
    String exported = manager.export(record);

    assertEquals("0000123456", exported.substring(24, 34));
  }

  // ── Plain POJO with explicit getters (no Lombok) ─────────────────────────────

  @Test
  void testPlainFieldAnnotation_load() {
    PlainFieldRecord record = manager.load(PlainFieldRecord.class, "Alice     00028");

    assertEquals("Alice", record.getName());
    assertEquals(28, record.getAge());
  }

  @Test
  void testPlainFieldAnnotation_export() {
    PlainFieldRecord record = new PlainFieldRecord();
    record.setName("Alice");
    record.setAge(28);

    String exported = manager.export(record);

    assertEquals("Alice     00028", exported);
  }

  // ── Regression: existing method annotations must still work ──────────────────

  @Test
  void testMethodAnnotation_stillWorks() {
    MethodAnnotatedRecord record = manager.load(MethodAnnotatedRecord.class, "Bob       ");

    assertEquals("Bob", record.getName());
    assertEquals("Bob       ", manager.export(record));
  }

  // ── Corner cases ─────────────────────────────────────────────────────────────

  @Test
  void testConflict_bothFieldAndGetterAnnotated_noExceptionThrown() {
    // When both field and getter carry @Field an error is logged and the field annotation is used.
    // No exception should be thrown.
    assertDoesNotThrow(() -> {
      ConflictRecord record = manager.load(ConflictRecord.class, "Hello     ");
      assertNotNull(record.getName());
    });
  }

  @Test
  void testConflict_fieldAnnotationWins() {
    // Field annotation: length=5; getter annotation: length=10
    // Export "Hi" — if field annotation wins result is 5 chars, if getter wins 10 chars.
    ConflictRecord record = new ConflictRecord();
    record.setName("Hi");
    String exported = manager.export(record);

    // Field annotation (length=5) wins → "Hi   " at offset 1
    assertEquals("Hi   ", exported.substring(0, 5));
  }

  @Test
  void testNoGetter_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class, () ->
        manager.load(NoGetterRecord.class, "Hello")
    );
  }

  @Test
  void testInheritedFieldAnnotation_load() {
    ChildRecord record = manager.load(ChildRecord.class, "Base  00099");

    assertEquals("Base", record.getCode());
    assertEquals(99, record.getCount());
  }

  @Test
  void testInheritedFieldAnnotation_export() {
    ChildRecord record = new ChildRecord();
    record.setCode("Base");
    record.setCount(99);

    String exported = manager.export(record);

    assertEquals("Base  00099", exported);
  }

  @Test
  void testNullValue_exportProducesPaddedField() {
    PlainFieldRecord record = new PlainFieldRecord();
    record.setName(null);
    record.setAge(0);

    String exported = manager.export(record);

    // null name → 10 spaces
    assertEquals("          ", exported.substring(0, 10));
  }

  // ── Mixed method + field annotations in same class ───────────────────────────

  @Test
  void testMixedAnnotations_load() {
    MixedAnnotationRecord record = manager.load(MixedAnnotationRecord.class, "Hello00042");

    assertEquals("Hello", record.getCode());
    assertEquals(42, record.getCount());
  }

  @Test
  void testMixedAnnotations_export() {
    MixedAnnotationRecord record = new MixedAnnotationRecord();
    record.setCode("Hello");
    record.setCount(42);

    assertEquals("Hello00042", manager.export(record));
  }

  // ── Helper record classes ─────────────────────────────────────────────────────

  private LombokRecord buildLombokRecord() {
    LombokRecord record = new LombokRecord();
    record.setName("Jacob");
    record.setAge(42);
    try {
      record.setBirthDate(new SimpleDateFormat("yyyyMMdd").parse("19850315"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    record.setActive(true);
    record.setSalary(new BigDecimal("1234.56"));
    return record;
  }

  /** Plain POJO — field annotations with explicit getters/setters (no Lombok). */
  @Record
  public static class PlainFieldRecord {

    @Field(offset = 1, length = 10)
    private String name;

    @Field(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0')
    private Integer age;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
  }

  /** Record using method annotations — verifies existing behaviour is unaffected. */
  @Record
  public static class MethodAnnotatedRecord {
    private String name;

    @Field(offset = 1, length = 10)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  /** Both field and getter annotated — field annotation should win, error logged. */
  @Record
  public static class ConflictRecord {

    @Field(offset = 1, length = 5)
    private String name;

    @Field(offset = 1, length = 10)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  /** Field annotation present but no getter exists — should throw FixedFormatException. */
  @Record
  public static class NoGetterRecord {

    @Field(offset = 1, length = 5)
    private String name;

    public void setName(String name) { this.name = name; }
  }

  /** One property uses method annotation, another uses field annotation in the same class. */
  @Record
  public static class MixedAnnotationRecord {

    private String code;

    @Field(offset = 1, length = 5)
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    @Field(offset = 6, length = 5, align = Align.RIGHT, paddingChar = '0')
    private Integer count;
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
  }

  /** Base class with field annotations — tests inheritance support. */
  @Record
  public static class BaseRecord {

    @Field(offset = 1, length = 6)
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
  }

  /** Subclass adds its own field annotation — both should be picked up. */
  @Record
  public static class ChildRecord extends BaseRecord {

    @Field(offset = 7, length = 5, align = Align.RIGHT, paddingChar = '0')
    private Integer count;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
  }
}
