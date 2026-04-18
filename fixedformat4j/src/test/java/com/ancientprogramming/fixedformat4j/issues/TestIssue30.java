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

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies Issue 30 — record-level default alignment via {@link Record#align()}.
 *
 * <p>When {@code @Record(align = Align.RIGHT)} is set, all fields that do not specify
 * an explicit {@code align=} on {@code @Field} inherit {@code Align.RIGHT}.
 * Fields with an explicit {@code @Field(align = ...)} always use their own setting.
 * Existing records without {@code @Record(align = ...)} continue to default to
 * {@code Align.LEFT} as before.
 *
 * @since 1.7.1
 */
public class TestIssue30 {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // ---------------------------------------------------------------------------
  // Record-level RIGHT alignment: fields inherit it when no align= is specified
  // ---------------------------------------------------------------------------

  @Test
  public void load_recordAlignRight_fieldWithoutAlignUsesRight() {
    DefaultAlignRecord30 loaded = manager.load(DefaultAlignRecord30.class, "0004200001");
    assertEquals(Integer.valueOf(42), loaded.getFirst());
    assertEquals(Integer.valueOf(1), loaded.getSecond());
  }

  @Test
  public void export_recordAlignRight_fieldWithoutAlignUsesRight() {
    DefaultAlignRecord30 record = new DefaultAlignRecord30();
    record.setFirst(42);
    record.setSecond(1);
    assertEquals("0004200001", manager.export(record));
  }

  @Test
  public void roundTrip_recordAlignRight_preservesValues() {
    DefaultAlignRecord30 original = new DefaultAlignRecord30();
    original.setFirst(7);
    original.setSecond(99);
    DefaultAlignRecord30 reloaded = manager.load(DefaultAlignRecord30.class, manager.export(original));
    assertEquals(Integer.valueOf(7), reloaded.getFirst());
    assertEquals(Integer.valueOf(99), reloaded.getSecond());
  }

  // ---------------------------------------------------------------------------
  // Explicit field align= overrides the record-level default
  // ---------------------------------------------------------------------------

  @Test
  public void load_recordAlignRight_explicitFieldAlignLeftWins() {
    MixedAlignRecord30 loaded = manager.load(MixedAlignRecord30.class, "00042AB   ");
    assertEquals(Integer.valueOf(42), loaded.getNumber());
    assertEquals("AB", loaded.getLabel());
  }

  @Test
  public void export_recordAlignRight_explicitFieldAlignLeftWins() {
    MixedAlignRecord30 record = new MixedAlignRecord30();
    record.setNumber(42);
    record.setLabel("AB");
    assertEquals("00042AB   ", manager.export(record));
  }

  // ---------------------------------------------------------------------------
  // Backward compatibility: @Record without align= still defaults to LEFT
  // ---------------------------------------------------------------------------

  @Test
  public void load_recordWithoutAlign_defaultsToLeft() {
    BackwardCompatRecord30 loaded = manager.load(BackwardCompatRecord30.class, "AB   CD   ");
    assertEquals("AB", loaded.getFirst());
    assertEquals("CD", loaded.getSecond());
  }

  @Test
  public void export_recordWithoutAlign_defaultsToLeft() {
    BackwardCompatRecord30 record = new BackwardCompatRecord30();
    record.setFirst("AB");
    record.setSecond("CD");
    assertEquals("AB   CD   ", manager.export(record));
  }

  // ---------------------------------------------------------------------------
  // Record definitions
  // ---------------------------------------------------------------------------

  /**
   * Two integer fields both inheriting {@code Align.RIGHT} from the record.
   * <pre>
   * offset 1, len 5  Integer  paddingChar='0'  (inherits RIGHT from @Record)
   * offset 6, len 5  Integer  paddingChar='0'  (inherits RIGHT from @Record)
   * </pre>
   */
  @Record(length = 10, align = Align.RIGHT)
  public static class DefaultAlignRecord30 {

    private Integer first;
    private Integer second;

    @Field(offset = 1, length = 5, paddingChar = '0')
    public Integer getFirst() { return first; }
    public void setFirst(Integer first) { this.first = first; }

    @Field(offset = 6, length = 5, paddingChar = '0')
    public Integer getSecond() { return second; }
    public void setSecond(Integer second) { this.second = second; }
  }

  /**
   * Record default is RIGHT; label field explicitly overrides to LEFT.
   * <pre>
   * offset 1, len 5  Integer  paddingChar='0'  (inherits RIGHT from @Record)
   * offset 6, len 5  String   paddingChar=' '  explicit align=LEFT
   * </pre>
   */
  @Record(length = 10, align = Align.RIGHT)
  public static class MixedAlignRecord30 {

    private Integer number;
    private String label;

    @Field(offset = 1, length = 5, paddingChar = '0')
    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }

    @Field(offset = 6, length = 5, align = Align.LEFT)
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
  }

  /**
   * No {@code align=} on {@code @Record} — both fields default to LEFT as before 1.7.1.
   * <pre>
   * offset 1, len 5  String  (defaults to LEFT)
   * offset 6, len 5  String  (defaults to LEFT)
   * </pre>
   */
  @Record(length = 10)
  public static class BackwardCompatRecord30 {

    private String first;
    private String second;

    @Field(offset = 1, length = 5)
    public String getFirst() { return first; }
    public void setFirst(String first) { this.first = first; }

    @Field(offset = 6, length = 5)
    public String getSecond() { return second; }
    public void setSecond(String second) { this.second = second; }
  }
}
