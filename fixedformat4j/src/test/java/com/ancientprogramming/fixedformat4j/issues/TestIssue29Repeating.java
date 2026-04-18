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
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that {@code nullChar} is applied per-element for repeating fields ({@code count > 1}).
 *
 * <p>A slice for an individual element that consists entirely of {@code nullChar} characters
 * is loaded as {@code null}. On export, a {@code null} element is written as
 * {@code length} copies of {@code nullChar}. The default {@code nullChar} ({@code '\0'})
 * keeps existing repeating-field behaviour unchanged.
 *
 * @since 1.7.1
 */
public class TestIssue29Repeating {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // ---------------------------------------------------------------------------
  // Load: all-nullChar element slice -> null element in list
  // ---------------------------------------------------------------------------

  @Test
  public void loadFirstElementAllNullChar_returnsNullAtIndex0() {
    // "     00042" — first slot all spaces (null), second slot "00042" (42)
    RepeatingNullableRecord loaded = manager.load(RepeatingNullableRecord.class, "     00042");
    assertNull(loaded.getValues().get(0));
    assertEquals(Integer.valueOf(42), loaded.getValues().get(1));
  }

  @Test
  public void loadSecondElementAllNullChar_returnsNullAtIndex1() {
    // "00042     " — first slot "00042" (42), second slot all spaces (null)
    RepeatingNullableRecord loaded = manager.load(RepeatingNullableRecord.class, "00042     ");
    assertEquals(Integer.valueOf(42), loaded.getValues().get(0));
    assertNull(loaded.getValues().get(1));
  }

  @Test
  public void loadBothElementsAllNullChar_returnsTwoNulls() {
    RepeatingNullableRecord loaded = manager.load(RepeatingNullableRecord.class, "          ");
    assertNull(loaded.getValues().get(0));
    assertNull(loaded.getValues().get(1));
  }

  @Test
  public void loadNoNullCharElements_parsesBothNormally() {
    RepeatingNullableRecord loaded = manager.load(RepeatingNullableRecord.class, "0000100025");
    assertEquals(Integer.valueOf(1), loaded.getValues().get(0));
    assertEquals(Integer.valueOf(25), loaded.getValues().get(1));
  }

  // ---------------------------------------------------------------------------
  // Export: null element -> nullChar fill
  // ---------------------------------------------------------------------------

  @Test
  public void exportFirstElementNull_emitsNullCharInFirstSlot() {
    RepeatingNullableRecord record = new RepeatingNullableRecord();
    record.setValues(Arrays.asList(null, 42));
    String exported = manager.export(record);
    assertEquals("     ", exported.substring(0, 5));
    assertEquals("00042", exported.substring(5, 10));
  }

  @Test
  public void exportSecondElementNull_emitsNullCharInSecondSlot() {
    RepeatingNullableRecord record = new RepeatingNullableRecord();
    record.setValues(Arrays.asList(42, null));
    String exported = manager.export(record);
    assertEquals("00042", exported.substring(0, 5));
    assertEquals("     ", exported.substring(5, 10));
  }

  @Test
  public void exportBothElementsNull_emitsNullCharInBothSlots() {
    RepeatingNullableRecord record = new RepeatingNullableRecord();
    record.setValues(Arrays.asList(null, null));
    String exported = manager.export(record);
    assertEquals("          ", exported);
  }

  // ---------------------------------------------------------------------------
  // Round-trip symmetry
  // ---------------------------------------------------------------------------

  @Test
  public void roundTripMixedNullAndValues() {
    RepeatingNullableRecord original = new RepeatingNullableRecord();
    original.setValues(Arrays.asList(null, 42));

    String exported = manager.export(original);
    RepeatingNullableRecord reloaded = manager.load(RepeatingNullableRecord.class, exported);

    assertNull(reloaded.getValues().get(0));
    assertEquals(Integer.valueOf(42), reloaded.getValues().get(1));
  }

  @Test
  public void roundTripAllNulls() {
    RepeatingNullableRecord original = new RepeatingNullableRecord();
    original.setValues(Arrays.asList(null, null));

    String exported = manager.export(original);
    RepeatingNullableRecord reloaded = manager.load(RepeatingNullableRecord.class, exported);

    assertNull(reloaded.getValues().get(0));
    assertNull(reloaded.getValues().get(1));
  }

  @Test
  public void roundTripNoNulls() {
    RepeatingNullableRecord original = new RepeatingNullableRecord();
    original.setValues(Arrays.asList(1, 99));

    String exported = manager.export(original);
    RepeatingNullableRecord reloaded = manager.load(RepeatingNullableRecord.class, exported);

    assertEquals(Integer.valueOf(1), reloaded.getValues().get(0));
    assertEquals(Integer.valueOf(99), reloaded.getValues().get(1));
  }

  // ---------------------------------------------------------------------------
  // Backward compatibility: no nullChar set -> existing behaviour preserved
  // ---------------------------------------------------------------------------

  @Test
  public void noNullCharSet_existingBehaviourPreserved() {
    // Without nullChar, all-spaces parses as "" (empty string stripped of padding), not null
    LegacyRepeatingRecord loaded = manager.load(LegacyRepeatingRecord.class, "     hello");
    assertEquals("", loaded.getValues().get(0));
    assertEquals("hello", loaded.getValues().get(1));
  }

  // ---------------------------------------------------------------------------
  // Record definitions
  // ---------------------------------------------------------------------------

  /**
   * Two Integer slots of width 5 each; nullChar=' ', paddingChar='0'.
   * Total length 10.
   *
   * @since 1.7.1
   */
  @Record(length = 10)
  public static class RepeatingNullableRecord {

    private List<Integer> values;

    @Field(offset = 1, length = 5, count = 2, align = Align.RIGHT, paddingChar = '0', nullChar = ' ')
    public List<Integer> getValues() {
      return values;
    }

    public void setValues(List<Integer> values) {
      this.values = values;
    }
  }

  /**
   * Two String slots of width 5 each; no nullChar (default behaviour).
   * Total length 10.
   *
   * @since 1.7.1
   */
  @Record(length = 10)
  public static class LegacyRepeatingRecord {

    private List<String> values;

    @Field(offset = 1, length = 5, count = 2, align = Align.LEFT, paddingChar = ' ')
    public List<String> getValues() {
      return values;
    }

    public void setValues(List<String> values) {
      this.values = values;
    }
  }
}
