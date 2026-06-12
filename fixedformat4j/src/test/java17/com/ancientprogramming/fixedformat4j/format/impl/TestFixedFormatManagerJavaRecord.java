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

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Fields;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatEnum;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.annotation.Sign;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Java {@code record} support — load and export of {@code @Record}-annotated record classes
 * through the canonical constructor (issue #119).
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
public class TestFixedFormatManagerJavaRecord {

  private final FixedFormatManager manager = FixedFormatManagerImpl.create();

  @Record
  record BasicRecord(
      @Field(offset = 1, length = 10) String text,
      @Field(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0') Integer amount) {}

  @Test
  public void loadSimpleRecord() {
    BasicRecord loaded = manager.load(BasicRecord.class, "some text 00123");
    assertEquals("some text", loaded.text());
    assertEquals(123, loaded.amount());
  }

  @Test
  public void exportSimpleRecord() {
    assertEquals("some text 00123", manager.export(new BasicRecord("some text", 123)));
  }

  @Test
  public void roundTripUsesRecordValueEquality() {
    BasicRecord original = new BasicRecord("some text", 123);
    assertEquals(original, manager.load(BasicRecord.class, manager.export(original)));
  }

  @Record
  record PrimitiveRecord(
      @Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0') int amount,
      @Field(offset = 6, length = 1) boolean active) {}

  @Test
  public void primitiveComponentsRoundTrip() {
    PrimitiveRecord original = new PrimitiveRecord(42, true);
    String exported = manager.export(original);
    assertEquals("00042T", exported);
    assertEquals(original, manager.load(PrimitiveRecord.class, exported));
  }

  @Record
  record ExtraConstructorRecord(
      @Field(offset = 1, length = 10) String name,
      @Field(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0') Integer amount) {

    ExtraConstructorRecord(String name) {
      this(name, -1);
    }
  }

  @Test
  public void loadUsesCanonicalConstructorWhenOthersExist() {
    ExtraConstructorRecord loaded = manager.load(ExtraConstructorRecord.class, "abc       00007");
    assertEquals(new ExtraConstructorRecord("abc", 7), loaded);
  }

  /**
   * Component names starting with {@code is}/{@code get} must not be mangled by the
   * getter-prefix-stripping applied to conventional POJO accessors.
   */
  @Record
  record PrefixNamedComponents(
      @Field(offset = 1, length = 10) String issuer,
      @Field(offset = 11, length = 10) String getaway) {}

  @Test
  public void componentNamesStartingWithAccessorPrefixesAreNotMangled() {
    PrefixNamedComponents original = new PrefixNamedComponents("BANK-0001", "north gate");
    assertEquals(original, manager.load(PrefixNamedComponents.class, manager.export(original)));
  }

  @Record
  record InnerRecord(@Field(offset = 1, length = 5) String code) {}

  @Record
  record OuterRecord(
      @Field(offset = 1, length = 5) InnerRecord inner,
      @Field(offset = 6, length = 5) String name) {}

  @Test
  public void nestedRecordComponentRoundTrips() {
    OuterRecord original = new OuterRecord(new InnerRecord("AB123"), "neo");
    String exported = manager.export(original);
    assertEquals("AB123neo  ", exported);
    assertEquals(original, manager.load(OuterRecord.class, exported));
  }

  @Record
  record MultiFormatRecord(
      @Fields({
          @Field(offset = 1, length = 8),
          @Field(offset = 9, length = 8)
      }) String code) {}

  @Test
  public void multiFormatFieldsLoadFirstAndExportAll() {
    MultiFormatRecord loaded = manager.load(MultiFormatRecord.class, "ABCDEFGH12345678");
    assertEquals("ABCDEFGH", loaded.code());
    assertEquals("ABCDEFGHABCDEFGH", manager.export(new MultiFormatRecord("ABCDEFGH")));
  }

  @Record
  record PartiallyAnnotatedRecord(
      @Field(offset = 1, length = 5) String code,
      String comment,
      int revision) {}

  @Test
  public void unannotatedComponentsGetNullOrPrimitiveDefault() {
    PartiallyAnnotatedRecord loaded = manager.load(PartiallyAnnotatedRecord.class, "AB123");
    assertEquals("AB123", loaded.code());
    assertNull(loaded.comment());
    assertEquals(0, loaded.revision());
  }

  record UnannotatedRecord(@Field(offset = 1, length = 5) String code) {}

  @Test
  public void recordWithoutRecordAnnotationIsRejected() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(UnannotatedRecord.class, "AB123"));
    assertTrue(ex.getMessage().contains("has to be marked with the record annotation"),
        "unexpected message: " + ex.getMessage());
  }

  @Record
  record RepeatingFieldRecord(
      @Field(offset = 1, length = 3, count = 3, align = Align.RIGHT, paddingChar = '0') List<Integer> values) {}

  @Test
  public void repeatingFieldComponentRoundTrips() {
    RepeatingFieldRecord original = new RepeatingFieldRecord(List.of(1, 2, 3));
    String exported = manager.export(original);
    assertEquals("001002003", exported);
    assertEquals(original, manager.load(RepeatingFieldRecord.class, exported));
  }

  @Record
  record NullableComponentRecord(
      @Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0', nullChar = '?') Integer count,
      @Field(offset = 6, length = 4, align = Align.RIGHT, paddingChar = '0', nullValue = "9998") Integer rate) {}

  @Test
  public void nullCharAndNullValueComponentsRoundTripNull() {
    NullableComponentRecord original = new NullableComponentRecord(null, null);
    String exported = manager.export(original);
    assertEquals("?????9998", exported);
    assertEquals(original, manager.load(NullableComponentRecord.class, exported));
  }

  @Test
  public void nullCharAndNullValueComponentsRoundTripValues() {
    NullableComponentRecord original = new NullableComponentRecord(12, 34);
    String exported = manager.export(original);
    assertEquals("000120034", exported);
    assertEquals(original, manager.load(NullableComponentRecord.class, exported));
  }

  enum Status { ACTIVE, CLOSED }

  /** Every supplementary annotation must apply to record components, exactly as on POJO getters. */
  @Record
  record SupplementaryAnnotationsRecord(
      @Field(offset = 1, length = 8) @FixedFormatPattern("yyyyMMdd") LocalDate date,
      @Field(offset = 9, length = 10, align = Align.RIGHT, paddingChar = '0') @FixedFormatDecimal(decimals = 2) BigDecimal price,
      @Field(offset = 19, length = 5, align = Align.RIGHT, paddingChar = '0') @FixedFormatNumber(sign = Sign.PREPEND) Integer delta,
      @Field(offset = 24, length = 1) @FixedFormatBoolean(trueValue = "Y", falseValue = "N") Boolean active,
      @Field(offset = 25, length = 6) @FixedFormatEnum(EnumFormat.LITERAL) Status status) {}

  @Test
  public void supplementaryAnnotationsApplyToRecordComponents() {
    SupplementaryAnnotationsRecord original = new SupplementaryAnnotationsRecord(
        LocalDate.of(2026, 4, 5), new BigDecimal("50.10"), -12, true, Status.CLOSED);

    String exported = manager.export(original);
    assertEquals("20260405" + "0000005010" + "-0012" + "Y" + "CLOSED", exported);

    SupplementaryAnnotationsRecord loaded = manager.load(SupplementaryAnnotationsRecord.class, exported);
    assertEquals(LocalDate.of(2026, 4, 5), loaded.date());
    assertEquals(0, new BigDecimal("50.10").compareTo(loaded.price()));
    assertEquals(-12, loaded.delta());
    assertEquals(true, loaded.active());
    assertEquals(Status.CLOSED, loaded.status());
  }
}
