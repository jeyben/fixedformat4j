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
import com.ancientprogramming.fixedformat4j.annotation.Fields;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public class TestFixedFormatManagerImpl {

  private static final Logger LOG = LoggerFactory.getLogger(TestFixedFormatManagerImpl.class);

  private static String STR = "some text ";

  public static final String MY_RECORD_DATA = "some text 0012320080514CT001100000010350000002056-0012 01200000002056";
  public static final String MULTIBLE_RECORD_DATA = "some      2008101320081013                       0100";
  public static final String MULTIBLE_RECORD_DATA_X_PADDED = "some      2008101320081013xxxxxxxxxxxxxxxxxxxxxxx0100";

  FixedFormatManager manager = null;

  @BeforeEach
  public void setUp() {
    manager = new FixedFormatManagerImpl();
  }

  @Test
  public void testLoadRecord() {
    MyRecord loadedRecord = manager.load(MyRecord.class, MY_RECORD_DATA);
    assertNotNull(loadedRecord);
    assertEquals(STR, loadedRecord.getStringData());
    assertTrue(loadedRecord.isBooleanData());
  }

  @Test
  public void testLoadMultibleFieldsRecord() {
    //when reading data having multible field annotations the first field will decide what data to return
    Calendar someDay = Calendar.getInstance();
    someDay.set(2008, 9, 13, 0, 0, 0);
    someDay.set(Calendar.MILLISECOND, 0);
    MultibleFieldsRecord loadedRecord = manager.load(MultibleFieldsRecord.class, MULTIBLE_RECORD_DATA);
    assertNotNull(loadedRecord);
    assertEquals("some      ", loadedRecord.getStringData());
    assertEquals(someDay.getTime(), loadedRecord.getDateData());
  }

  @Test
  public void testExportRecordObject() {
    MyRecord myRecord = createMyRecord();
    assertEquals(MY_RECORD_DATA, manager.export(myRecord), "wrong record exported");
  }

  @Test
  public void testExportNestedRecordObject() {
    MyRecord myRecord = createMyRecord();
    MyOtherRecord myOtherRecord = new MyOtherRecord(myRecord);
    assertEquals(MY_RECORD_DATA, manager.export(myOtherRecord), "wrong record exported");

    myOtherRecord = new MyOtherRecord((MyRecord) null);
    assertEquals("", manager.export(myOtherRecord), "wrong record exported");
  }

  private MyRecord createMyRecord() {
    Calendar someDay = Calendar.getInstance();
    someDay.set(2008, 4, 14, 0, 0, 0);
    someDay.set(Calendar.MILLISECOND, 0);

    MyRecord myRecord = new MyRecord();
    myRecord.setBooleanData(true);
    myRecord.setCharData('C');
    myRecord.setDateData(someDay.getTime());
    myRecord.setDoubleData(10.35);
    myRecord.setFloatData(20.56F);
    myRecord.setLongData(11L);
    myRecord.setIntegerData(123);
    myRecord.setStringData("some text ");
    myRecord.setBigDecimalData(new BigDecimal(-12.012));
    myRecord.setSimpleFloatData(20.56F);
    return myRecord;
  }

  @Test
  public void testExportMultibleFieldRecordObject() {
    Calendar someDay = Calendar.getInstance();
    someDay.set(2008, 9, 13, 0, 0, 0);
    someDay.set(Calendar.MILLISECOND, 0);

    MultibleFieldsRecord multibleFieldsRecord = new MultibleFieldsRecord();
    multibleFieldsRecord.setDateData(someDay.getTime());
    multibleFieldsRecord.setStringData("some      ");
    multibleFieldsRecord.setIntegerdata(100);
    manager.export(multibleFieldsRecord);
    assertEquals(MULTIBLE_RECORD_DATA, manager.export(multibleFieldsRecord), "wrong record exported");
  }

  @Test
  public void testExportIntoExistingString() {
    Calendar someDay = Calendar.getInstance();
    someDay.set(2008, 9, 13, 0, 0, 0);
    someDay.set(Calendar.MILLISECOND, 0);

    MultibleFieldsRecord multibleFieldsRecord = new MultibleFieldsRecord();
    multibleFieldsRecord.setDateData(someDay.getTime());
    multibleFieldsRecord.setStringData("some      ");
    multibleFieldsRecord.setIntegerdata(100);
    String exportedString = manager.export("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx", multibleFieldsRecord);
    assertEquals(MULTIBLE_RECORD_DATA_X_PADDED, exportedString, "wrong record exported");
  }

  @Test
  public void testLoadNonRecordAnnotatedClass() {
    assertThrows(FixedFormatException.class, () -> manager.load(String.class, "some"));
  }

  @Test
  public void testExportAnnotatedNestedClass() {
    MyRecord.MyStaticNestedClass myStaticNestedClass = new MyRecord.MyStaticNestedClass();
    myStaticNestedClass.setStringData("xyz");
    String exportedString = manager.export(myStaticNestedClass);
    assertEquals("xyz       ", exportedString);

    NoDefaultConstructorClass.MyStaticNestedClass myStaticNestedClass2 = new NoDefaultConstructorClass.MyStaticNestedClass();
    myStaticNestedClass2.setStringData("xyz");
    String exportedString2 = manager.export(myStaticNestedClass2);
    assertEquals("xyz       ", exportedString2);
  }

  @Test
  public void testExportAnnotatedInnerClass() {
    MyRecord myRecord = new MyRecord();
    MyRecord.MyInnerClass myInnerClass = myRecord.new MyInnerClass();
    myInnerClass.setStringData("xyz");
    String exportedString = manager.export(myInnerClass);
    assertEquals("xyz       ", exportedString);

    NoDefaultConstructorClass noDefaultConstructorClass = new NoDefaultConstructorClass("foobar");
    NoDefaultConstructorClass.MyInnerClass myInnerClass2 = noDefaultConstructorClass.new MyInnerClass();
    myInnerClass2.setStringData("xyz");
    exportedString = manager.export(myInnerClass2);
    assertEquals("xyz       ", exportedString);
  }

  @Test
  public void testImportAnnotatedNestedClass() {
    MyRecord.MyStaticNestedClass staticNested = manager.load(MyRecord.MyStaticNestedClass.class, "xyz       ");
    assertEquals("xyz", staticNested.getStringData());

    NoDefaultConstructorClass.MyStaticNestedClass staticNested2 = manager.load(NoDefaultConstructorClass.MyStaticNestedClass.class, "xyz       ");
    assertEquals("xyz", staticNested2.getStringData());
  }

  @Test
  public void testImportAnnotatedInnerClass() {
    MyRecord.MyInnerClass inner = manager.load(MyRecord.MyInnerClass.class, "xyz       ");
    assertEquals("xyz", inner.getStringData());

    assertThrows(FixedFormatException.class, () ->
      manager.load(NoDefaultConstructorClass.MyInnerClass.class, "xyz       ")
    );
  }

  @Test
  public void testParseFail() {
    assertThrows(ParseException.class, () ->
      manager.load(MyRecord.class, "foobarfoobarfoobarfoobar")
    );
  }

  @Test
  public void testRecordLengthPadsExportedString() {
    FixedLengthRecord rec = new FixedLengthRecord();
    rec.setName("hi");
    String exported = manager.export(rec);
    assertEquals(10, exported.length());
    assertEquals("hi        ", exported);
  }

  @Test
  public void testRecordCustomPaddingChar() {
    PaddedRecord rec = new PaddedRecord();
    rec.setName("hi");
    String exported = manager.export(rec);
    assertEquals(10, exported.length());
    assertTrue(exported.startsWith("hi"));
    assertEquals("hi********", exported);
  }

  @Test
  public void testLocalDateRoundTrip() {
    LocalDateRecord rec = new LocalDateRecord();
    rec.setEventDate(LocalDate.of(2026, 4, 5));
    rec.setLabel("launch");
    String exported = manager.export(rec);
    assertEquals("launch    20260405", exported);

    LocalDateRecord loaded = manager.load(LocalDateRecord.class, exported);
    assertEquals("launch", loaded.getLabel());
    assertEquals(LocalDate.of(2026, 4, 5), loaded.getEventDate());
  }

  @Test
  public void testLocalDateNullRoundTrip() {
    LocalDateRecord rec = new LocalDateRecord();
    rec.setLabel("test");
    // null date → 8 spaces in the string
    String exported = manager.export(rec);
    assertEquals("test      " + "        ", exported);

    LocalDateRecord loaded = manager.load(LocalDateRecord.class, exported);
    assertNull(loaded.getEventDate());
  }

  @Test
  public void testIsPrefixedBooleanGetterRoundTrip() {
    BooleanRecord rec = new BooleanRecord();
    rec.setActive(true);
    String exported = manager.export(rec);
    assertEquals("T", exported);
    BooleanRecord loaded = manager.load(BooleanRecord.class, "T");
    assertTrue(loaded.isActive());
  }

  @Record
  public static class LocalDateRecord {
    private String label;
    private LocalDate eventDate;

    @Field(offset = 1, length = 10)
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    @Field(offset = 11, length = 8)
    @FixedFormatPattern("yyyyMMdd")
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
  }

  // --- Helper record classes for edge case tests ---

  @Record(length = 10)
  public static class FixedLengthRecord {
    private String name;

    @Field(offset = 1, length = 5)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  @Record(length = 10, paddingChar = '*')
  public static class PaddedRecord {
    private String name;

    @Field(offset = 1, length = 2)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  @Record
  public static class BooleanRecord {
    private boolean active;

    @com.ancientprogramming.fixedformat4j.annotation.Field(offset = 1, length = 1)
    @com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean(trueValue = "T", falseValue = "F")
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
  }

  // --- Default-pattern fallback tests ---

  @Test
  public void testLocalDateNoPatternAnnotationUsesIsoLocalDateDefault() {
    LocalDateNoPatternRecord rec = new LocalDateNoPatternRecord();
    rec.setEventDate(LocalDate.of(2026, 4, 9));
    String exported = manager.export(rec);
    assertEquals("2026-04-09", exported);

    LocalDateNoPatternRecord loaded = manager.load(LocalDateNoPatternRecord.class, exported);
    assertEquals(LocalDate.of(2026, 4, 9), loaded.getEventDate());
  }

  @Record
  public static class LocalDateNoPatternRecord {
    private LocalDate eventDate;

    @Field(offset = 1, length = 10)
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
  }

  // --- LocalDateTime round-trip tests ---

  @Test
  public void testLocalDateTimeRoundTrip() {
    LocalDateTimeRecord rec = new LocalDateTimeRecord();
    rec.setEventAt(LocalDateTime.of(2026, 4, 9, 14, 30, 0));
    rec.setLabel("launch");
    String exported = manager.export(rec);
    assertEquals("launch    2026-04-09T14:30:00", exported);

    LocalDateTimeRecord loaded = manager.load(LocalDateTimeRecord.class, exported);
    assertEquals("launch", loaded.getLabel());
    assertEquals(LocalDateTime.of(2026, 4, 9, 14, 30, 0), loaded.getEventAt());
  }

  @Test
  public void testLocalDateTimeNoPatternAnnotationUsesIsoDefault() {
    LocalDateTimeNoPatternRecord rec = new LocalDateTimeNoPatternRecord();
    rec.setEventAt(LocalDateTime.of(2026, 4, 9, 14, 30, 0));
    String exported = manager.export(rec);
    assertEquals("2026-04-09T14:30:00", exported);

    LocalDateTimeNoPatternRecord loaded = manager.load(LocalDateTimeNoPatternRecord.class, exported);
    assertEquals(LocalDateTime.of(2026, 4, 9, 14, 30, 0), loaded.getEventAt());
  }

  @Record
  public static class LocalDateTimeRecord {
    private String label;
    private LocalDateTime eventAt;

    @Field(offset = 1, length = 10)
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    @Field(offset = 11, length = 19)
    @FixedFormatPattern("yyyy-MM-dd'T'HH:mm:ss")
    public LocalDateTime getEventAt() { return eventAt; }
    public void setEventAt(LocalDateTime eventAt) { this.eventAt = eventAt; }
  }

  @Record
  public static class LocalDateTimeNoPatternRecord {
    private LocalDateTime eventAt;

    @Field(offset = 1, length = 19)
    public LocalDateTime getEventAt() { return eventAt; }
    public void setEventAt(LocalDateTime eventAt) { this.eventAt = eventAt; }
  }

  // --- Pattern validation tests ---

  @Test
  public void testInvalidDatePatternOnLoadThrowsFixedFormatException() {
    assertThrows(FixedFormatException.class, () ->
        manager.load(InvalidDatePatternRecord.class, "20260409")
    );
  }

  @Test
  public void testInvalidDatePatternOnExportThrowsFixedFormatException() {
    InvalidDatePatternRecord rec = new InvalidDatePatternRecord();
    rec.setEventDate(new Date());
    assertThrows(FixedFormatException.class, () -> manager.export(rec));
  }

  @Test
  public void testInvalidLocalDatePatternOnLoadThrowsFixedFormatException() {
    assertThrows(FixedFormatException.class, () ->
        manager.load(InvalidLocalDatePatternRecord.class, "20260409")
    );
  }

  @Test
  public void testInvalidLocalDatePatternOnExportThrowsFixedFormatException() {
    InvalidLocalDatePatternRecord rec = new InvalidLocalDatePatternRecord();
    rec.setEventDate(LocalDate.of(2026, 4, 9));
    assertThrows(FixedFormatException.class, () -> manager.export(rec));
  }

  @Test
  public void testInvalidLocalDateTimePatternOnLoadThrowsFixedFormatException() {
    assertThrows(FixedFormatException.class, () ->
        manager.load(InvalidLocalDateTimePatternRecord.class, "2026-04-09T14:30:00")
    );
  }

  @Test
  public void testInvalidLocalDateTimePatternOnExportThrowsFixedFormatException() {
    InvalidLocalDateTimePatternRecord rec = new InvalidLocalDateTimePatternRecord();
    rec.setEventDateTime(LocalDateTime.of(2026, 4, 9, 14, 30, 0));
    assertThrows(FixedFormatException.class, () -> manager.export(rec));
  }

  @Record
  public static class InvalidDatePatternRecord {
    private Date eventDate;

    @Field(offset = 1, length = 8)
    @FixedFormatPattern("yyyyjj")
    public Date getEventDate() { return eventDate; }
    public void setEventDate(Date eventDate) { this.eventDate = eventDate; }
  }

  @Record
  public static class InvalidLocalDatePatternRecord {
    private LocalDate eventDate;

    @Field(offset = 1, length = 8)
    @FixedFormatPattern("yyyyjj")
    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }
  }

  @Record
  public static class InvalidLocalDateTimePatternRecord {
    private LocalDateTime eventDateTime;

    @Field(offset = 1, length = 19)
    @FixedFormatPattern("yyyyjj")
    public LocalDateTime getEventDateTime() { return eventDateTime; }
    public void setEventDateTime(LocalDateTime eventDateTime) { this.eventDateTime = eventDateTime; }
  }

  // --- Cluster B: load() edge paths ---

  @Test
  public void testLoad_fieldsAnnotation_onlyFirstFieldUsed() {
    // @Fields with two offsets having DISTINCT data — only offset=1 should be loaded
    TwoFieldsDifferentRecord rec = manager.load(TwoFieldsDifferentRecord.class, "ABCDEF");
    assertEquals("ABC", rec.getValue(),
        "Only the first @Field in @Fields should be loaded; got: " + rec.getValue());
  }

  @Test
  public void testLoad_nullCharAllMatch_fieldIsNull() {
    NullCharStringRecord rec = manager.load(NullCharStringRecord.class, "     ");
    assertNull(rec.getText(), "All nullChars in slice → field should be null");
  }

  @Test
  public void testLoad_nullCharPartialMatch_fieldIsParsed() {
    NullCharStringRecord rec = manager.load(NullCharStringRecord.class, "hel  ");
    assertNotNull(rec.getText(), "Partial nullChar match → field should not be null");
    assertEquals("hel", rec.getText());
  }

  @Test
  public void testLoad_nullCharInactive_spacesLoadNormally() {
    NoNullCharStringRecord rec = manager.load(NoNullCharStringRecord.class, "     ");
    // No nullChar → formatter strips trailing spaces → empty string (not null)
    assertEquals("", rec.getText());
  }

  @Test
  public void testLoad_readOnlyField_doesNotThrow() {
    ReadOnlyFieldRecord rec = manager.load(ReadOnlyFieldRecord.class, "hello");
    assertNotNull(rec, "Load of record with read-only field should not throw");
    assertNull(rec.getText(), "Read-only field has no setter — value stays at default null");
  }

  @Test
  public void testLoad_nestedRecord_recursivelyLoadedValue() {
    // Asserts that the recursively-loaded value is correct, not just that load didn't throw
    NestedContainerRecord rec = manager.load(NestedContainerRecord.class, "HELLO");
    assertNotNull(rec.getInner(), "Nested @Record field should be loaded recursively");
    assertEquals("HELLO", rec.getInner().getText(),
        "Recursively-loaded value should match the data slice");
  }

  @Test
  public void testLoad_nestedRecord_roundTrip() {
    NestedContainerRecord outer = new NestedContainerRecord();
    NestedInnerRecord inner = new NestedInnerRecord();
    inner.setText("WORLD");
    outer.setInner(inner);
    String exported = manager.export(outer);
    NestedContainerRecord loaded = manager.load(NestedContainerRecord.class, exported);
    assertNotNull(loaded.getInner());
    assertEquals("WORLD", loaded.getInner().getText());
  }

  // --- Cluster C: export() edge paths ---

  @Test
  public void testExport_nestedRecord_nullValue_outputsPadding() {
    NullableNestedRecord rec = new NullableNestedRecord();
    // inner == null → isNestedRecord=true, valueObject=null → padding only
    String exported = manager.export(rec);
    assertEquals("     ", exported,
        "Null nested @Record field should export as padding chars");
  }

  @Test
  public void testExport_nestedRecord_nonNullValue_recursivelyExported() {
    NullableNestedRecord rec = new NullableNestedRecord();
    NestedInnerRecord inner = new NestedInnerRecord();
    inner.setText("HI");
    rec.setInner(inner);
    String exported = manager.export(rec);
    assertEquals("HI   ", exported,
        "Non-null nested @Record field should be recursively exported");
  }

  @Test
  public void testExport_nullValue_nullCharActive_outputsNullChar() {
    NullCharExportRecord rec = new NullCharExportRecord();
    // value == null, nullChar='0' active → "00000"
    String exported = manager.export(rec);
    assertEquals("00000", exported,
        "Null value with active nullChar should export as repeated nullChar");
  }

  @Test
  public void testExport_nullValue_nullCharInactive_outputsDefault() {
    NoNullCharExportRecord rec = new NoNullCharExportRecord();
    // value == null, no nullChar → formatter handles null → "     " (space-padded)
    String exported = manager.export(rec);
    assertEquals("     ", exported,
        "Null value without nullChar should export as formatter default (space padding)");
  }

  @Test
  public void testExport_recordLengthExactBoundary_noPaddingAdded() {
    // Record length == exported string length: the padding while-loop must NOT execute
    ExactLengthRecord rec = new ExactLengthRecord();
    rec.setName("hello");
    String exported = manager.export(rec);
    assertEquals("hello", exported);
    assertEquals(5, exported.length());
  }

  @Test
  public void testExport_recordLengthUnbounded_noExtraPadding() {
    // @Record with default length=-1: no padding loop runs after export
    UnboundedRecord rec = new UnboundedRecord();
    rec.setName("hi");
    String exported = manager.export(rec);
    // field length=5, "hi" left-padded → "hi   "
    assertEquals("hi   ", exported);
  }

  @Test
  public void testExport_repeatingField_delegatesToRepeatingFieldSupport() {
    RepeatingFieldRecord rec = new RepeatingFieldRecord();
    rec.setCodes(new String[]{"AAA", "BBB", "CCC"});
    rec.setAmounts(new Integer[]{42, 99});
    String exported = manager.export(rec);
    assertEquals("AAA  BBB  CCC  0004200099", exported);
  }

  // --- Cluster D: appendData boundaries ---

  @Test
  public void testAppendData_fieldAtOffset1_noGapPaddingNeeded() {
    // zeroBasedOffset=0, result.length()==0 → while-loop does not execute
    GaplessRecord rec = new GaplessRecord();
    rec.setData("AB");
    String exported = manager.export(rec);
    assertEquals("AB   ", exported);
  }

  @Test
  public void testAppendData_fieldAtOffset5_gapIsPadded() {
    // zeroBasedOffset=4, result starts as "", while-loop pads 4 spaces
    GapAtOffset5Record rec = new GapAtOffset5Record();
    rec.setData("XY");
    String exported = manager.export(rec);
    // 4 spaces (gap) + "XY " (field, left-padded to 3 chars)
    assertEquals("    XY ", exported);
  }

  @Test
  public void testAppendData_twoNonAdjacentFields_gapFilledWithPadding() {
    // field A at offset 1 (length 3), field B at offset 8 (length 3)
    // gap between offset 4 and 7 should be filled with paddingChar
    TwoGapFieldsRecord rec = new TwoGapFieldsRecord();
    rec.setA("AA");
    rec.setB("BB");
    String exported = manager.export(rec);
    // "AA " + 4 spaces gap + "BB "
    assertEquals("AA     BB ", exported);
  }

  @Test
  public void testAppendData_exportWithTemplate_fieldsOverwriteTemplate() {
    // Uses export(template, record) to test that appendData replaces template chars
    TwoGapFieldsRecord rec = new TwoGapFieldsRecord();
    rec.setA("AA");
    rec.setB("BB");
    String exported = manager.export("xxxxxxxxxx", rec);
    // template "xxxxxxxxxx" (10 chars), field A at indices 0-2 → "AA ",
    // gap at indices 3-6 stays "xxxx", field B at indices 7-9 → "BB "
    assertEquals("AA xxxxBB ", exported);
  }

  // --- Cluster B/C: record annotation properties verified via behavior ---

  @Test
  public void testRecordAnnotation_paddingChar_usedForFieldGaps() {
    StarPaddedRecord rec = new StarPaddedRecord();
    rec.setData("AB");
    String exported = manager.export(rec);
    // Field at offset 5 (length 3), record paddingChar='*'
    // gap = "****" (4 stars) + field "AB " (left-padded to 3)
    // Wait, the field paddingChar default is ' '. But the GAP uses record.paddingChar().
    // The formatter pads the field value with fieldAnnotation.paddingChar() = ' ' (default).
    // appendData uses record.paddingChar()='*' for gap padding.
    // So: "****" (gap from appendData while-loop) + "AB " (formatted field, space-padded)
    assertEquals("****AB ", exported);
  }

  // --- Inner record classes for edge case tests ---

  @Record
  public static class TwoFieldsDifferentRecord {
    private String value;

    @Fields({@Field(offset = 1, length = 3), @Field(offset = 4, length = 3)})
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
  }

  @Record
  public static class NullCharStringRecord {
    private String text;

    @Field(offset = 1, length = 5, nullChar = ' ')
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
  }

  @Record
  public static class NoNullCharStringRecord {
    private String text;

    @Field(offset = 1, length = 5)
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
  }

  @Record
  public static class ReadOnlyFieldRecord {
    private String text;

    @Field(offset = 1, length = 5)
    public String getText() { return text; }
    // No setter — setterHandle will be null in FieldDescriptor
  }

  @Record
  public static class NestedInnerRecord {
    private String text;

    @Field(offset = 1, length = 5)
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
  }

  @Record
  public static class NestedContainerRecord {
    private NestedInnerRecord inner;

    @Field(offset = 1, length = 5)
    public NestedInnerRecord getInner() { return inner; }
    public void setInner(NestedInnerRecord inner) { this.inner = inner; }
  }

  @Record
  public static class NullableNestedRecord {
    private NestedInnerRecord inner;

    @Field(offset = 1, length = 5)
    public NestedInnerRecord getInner() { return inner; }
    public void setInner(NestedInnerRecord inner) { this.inner = inner; }
  }

  @Record
  public static class NullCharExportRecord {
    private Integer value;

    @Field(offset = 1, length = 5, nullChar = '0')
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
  }

  @Record
  public static class NoNullCharExportRecord {
    private String value;

    @Field(offset = 1, length = 5)
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
  }

  @Record(length = 5)
  public static class ExactLengthRecord {
    private String name;

    @Field(offset = 1, length = 5)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  @Record
  public static class UnboundedRecord {
    private String name;

    @Field(offset = 1, length = 5)
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
  }

  @Record
  public static class GaplessRecord {
    private String data;

    @Field(offset = 1, length = 5)
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
  }

  @Record
  public static class GapAtOffset5Record {
    private String data;

    @Field(offset = 5, length = 3)
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
  }

  @Record
  public static class TwoGapFieldsRecord {
    private String a;
    private String b;

    @Field(offset = 1, length = 3)
    public String getA() { return a; }
    public void setA(String a) { this.a = a; }

    @Field(offset = 8, length = 3)
    public String getB() { return b; }
    public void setB(String b) { this.b = b; }
  }

  @Record(paddingChar = '*')
  public static class StarPaddedRecord {
    private String data;

    @Field(offset = 5, length = 3)
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
  }
}
