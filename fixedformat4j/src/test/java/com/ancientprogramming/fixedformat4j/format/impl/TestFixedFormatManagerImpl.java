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
}
