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
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies Issue 29 - distinguish "no data" from zero/empty via an opt-in
 * {@code nullChar} attribute on {@link Field}.
 *
 * <p>When {@code nullChar} is set to a character different from {@code paddingChar},
 * the manager treats a field fully filled with {@code nullChar} as {@code null} on
 * load, and serializes {@code null} values as {@code length × nullChar} on export.
 * When {@code nullChar == paddingChar} (the default case), detection is disabled
 * and existing behavior is preserved.
 *
 * @since 1.7.1
 */
public class TestIssue29 {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // ---------------------------------------------------------------------------
  // Load: empty (all-nullChar) field -> null
  // ---------------------------------------------------------------------------

  @Test
  public void loadAllSpacesOnIntegerField_returnsNull() {
    NullableRecord29 loaded = manager.load(NullableRecord29.class, "     0000100000000AB**");
    assertNull(loaded.getIntegerData());
  }

  @Test
  public void loadAllSpacesOnLongField_returnsNull() {
    NullableRecord29 loaded = manager.load(NullableRecord29.class, "00042     00000000AB**");
    assertNull(loaded.getLongData());
  }

  @Test
  public void loadAllSpacesOnBigDecimalField_returnsNull() {
    NullableRecord29 loaded = manager.load(NullableRecord29.class, "0004200001        AB**");
    assertNull(loaded.getAmount());
  }

  @Test
  public void loadAllSpacesOnStringField_returnsNull() {
    NullableRecord29 loaded = manager.load(NullableRecord29.class, "000420000100000000    ");
    assertNull(loaded.getLabel());
  }

  // ---------------------------------------------------------------------------
  // Load: zero is still zero (null and zero are now distinguishable)
  // ---------------------------------------------------------------------------

  @Test
  public void loadAllZerosOnIntegerField_returnsZero() {
    NullableRecord29 loaded = manager.load(NullableRecord29.class, "000000000100000000AB**");
    assertNotNull(loaded.getIntegerData());
    assertEquals(Integer.valueOf(0), loaded.getIntegerData());
  }

  @Test
  public void loadAllZerosOnBigDecimalField_returnsZero() {
    NullableRecord29 loaded = manager.load(NullableRecord29.class, "000420000100000000AB**");
    assertNotNull(loaded.getAmount());
    assertEquals(0, loaded.getAmount().compareTo(BigDecimal.ZERO));
  }

  @Test
  public void loadZeroPaddedNumber_returnsParsedValue() {
    NullableRecord29 loaded = manager.load(NullableRecord29.class, "000420000100001234AB**");
    assertEquals(Integer.valueOf(42), loaded.getIntegerData());
    assertEquals(Long.valueOf(1L), loaded.getLongData());
    assertEquals(0, loaded.getAmount().compareTo(new BigDecimal("12.34")));
    assertEquals("AB", loaded.getLabel());
  }

  // ---------------------------------------------------------------------------
  // Export: null -> nullChar fill
  // ---------------------------------------------------------------------------

  @Test
  public void exportNullIntegerField_emitsSpaces() {
    NullableRecord29 record = new NullableRecord29();
    record.setIntegerData(null);
    record.setLongData(1L);
    record.setAmount(BigDecimal.ZERO);
    record.setLabel("AB");
    String exported = manager.export(record);
    assertEquals("     ", exported.substring(0, 5));
  }

  @Test
  public void exportNullBigDecimalField_emitsSpaces() {
    NullableRecord29 record = new NullableRecord29();
    record.setIntegerData(42);
    record.setLongData(1L);
    record.setAmount(null);
    record.setLabel("AB");
    String exported = manager.export(record);
    assertEquals("        ", exported.substring(10, 18));
  }

  @Test
  public void exportNullStringField_emitsSpaces() {
    NullableRecord29 record = new NullableRecord29();
    record.setIntegerData(42);
    record.setLongData(1L);
    record.setAmount(BigDecimal.ZERO);
    record.setLabel(null);
    String exported = manager.export(record);
    assertEquals("    ", exported.substring(18, 22));
  }

  // ---------------------------------------------------------------------------
  // Export: zero / empty are preserved (not confused with null)
  // ---------------------------------------------------------------------------

  @Test
  public void exportZeroIntegerField_emitsZeroPadded() {
    NullableRecord29 record = new NullableRecord29();
    record.setIntegerData(0);
    record.setLongData(1L);
    record.setAmount(BigDecimal.ZERO);
    record.setLabel("AB");
    String exported = manager.export(record);
    assertEquals("00000", exported.substring(0, 5));
  }

  @Test
  public void exportEmptyStringField_emitsPaddingChar() {
    NullableRecord29 record = new NullableRecord29();
    record.setIntegerData(42);
    record.setLongData(1L);
    record.setAmount(BigDecimal.ZERO);
    record.setLabel("");
    String exported = manager.export(record);
    assertEquals("****", exported.substring(18, 22));
  }

  // ---------------------------------------------------------------------------
  // Round-trip symmetry
  // ---------------------------------------------------------------------------

  @Test
  public void roundTripWithNullFields() {
    NullableRecord29 original = new NullableRecord29();
    original.setIntegerData(null);
    original.setLongData(null);
    original.setAmount(null);
    original.setLabel(null);

    String exported = manager.export(original);
    NullableRecord29 reloaded = manager.load(NullableRecord29.class, exported);

    assertNull(reloaded.getIntegerData());
    assertNull(reloaded.getLongData());
    assertNull(reloaded.getAmount());
    assertNull(reloaded.getLabel());
  }

  @Test
  public void roundTripWithZeroAndEmptyFields() {
    NullableRecord29 original = new NullableRecord29();
    original.setIntegerData(0);
    original.setLongData(0L);
    original.setAmount(BigDecimal.ZERO);
    original.setLabel("");

    String exported = manager.export(original);
    NullableRecord29 reloaded = manager.load(NullableRecord29.class, exported);

    assertEquals(Integer.valueOf(0), reloaded.getIntegerData());
    assertEquals(Long.valueOf(0L), reloaded.getLongData());
    assertEquals(0, reloaded.getAmount().compareTo(BigDecimal.ZERO));
    assertEquals("", reloaded.getLabel());
  }

  @Test
  public void roundTripWithMixedNullAndValueFields() {
    NullableRecord29 original = new NullableRecord29();
    original.setIntegerData(42);
    original.setLongData(null);
    original.setAmount(new BigDecimal("12.34"));
    original.setLabel(null);

    String exported = manager.export(original);
    NullableRecord29 reloaded = manager.load(NullableRecord29.class, exported);

    assertEquals(Integer.valueOf(42), reloaded.getIntegerData());
    assertNull(reloaded.getLongData());
    assertEquals(0, reloaded.getAmount().compareTo(new BigDecimal("12.34")));
    assertNull(reloaded.getLabel());
  }

  // ---------------------------------------------------------------------------
  // Backward compatibility: default Field annotation => detection disabled
  // ---------------------------------------------------------------------------

  @Test
  public void defaultFieldWithoutNullChar_behavesAsBefore() {
    // Both paddingChar and nullChar default to effectively "disabled",
    // so a space-padded empty field still parses to its formatter default.
    LegacyRecord29 loaded = manager.load(LegacyRecord29.class, "     ");
    assertEquals(Integer.valueOf(0), loaded.getIntegerData());
  }

  @Test
  public void nullCharEqualsPaddingChar_detectionDisabled() {
    // When explicitly set equal, detection must stay off.
    SameCharRecord29 loaded = manager.load(SameCharRecord29.class, "     ");
    assertEquals(Integer.valueOf(0), loaded.getIntegerData());
  }

  // ---------------------------------------------------------------------------
  // Record definitions
  // ---------------------------------------------------------------------------

  /**
   * Total length 22:
   * <pre>
   * offset  1, len 5  Integer    paddingChar='0', nullChar=' '
   * offset  6, len 5  Long       paddingChar='0', nullChar=' '
   * offset 11, len 8  BigDecimal paddingChar='0', nullChar=' ', decimals=2
   * offset 19, len 4  String     paddingChar='*', nullChar=' '
   * </pre>
   */
  @Record(length = 22)
  public static class NullableRecord29 {

    private Integer integerData;
    private Long longData;
    private BigDecimal amount;
    private String label;

    @Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0', nullChar = ' ')
    public Integer getIntegerData() {
      return integerData;
    }

    public void setIntegerData(Integer integerData) {
      this.integerData = integerData;
    }

    @Field(offset = 6, length = 5, align = Align.RIGHT, paddingChar = '0', nullChar = ' ')
    public Long getLongData() {
      return longData;
    }

    public void setLongData(Long longData) {
      this.longData = longData;
    }

    @Field(offset = 11, length = 8, align = Align.RIGHT, paddingChar = '0', nullChar = ' ')
    @FixedFormatDecimal(decimals = 2)
    public BigDecimal getAmount() {
      return amount;
    }

    public void setAmount(BigDecimal amount) {
      this.amount = amount;
    }

    @Field(offset = 19, length = 4, align = Align.LEFT, paddingChar = '*', nullChar = ' ')
    public String getLabel() {
      return label;
    }

    public void setLabel(String label) {
      this.label = label;
    }
  }

  @Record(length = 5)
  public static class LegacyRecord29 {

    private Integer integerData;

    @Field(offset = 1, length = 5)
    public Integer getIntegerData() {
      return integerData;
    }

    public void setIntegerData(Integer integerData) {
      this.integerData = integerData;
    }
  }

  @Record(length = 5)
  public static class SameCharRecord29 {

    private Integer integerData;

    @Field(offset = 1, length = 5, paddingChar = ' ', nullChar = ' ')
    public Integer getIntegerData() {
      return integerData;
    }

    public void setIntegerData(Integer integerData) {
      this.integerData = integerData;
    }
  }
}
