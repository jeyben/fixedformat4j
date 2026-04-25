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
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.ReadResult;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for Issue 97 &mdash; {@code @Field(length = -1)} rest-of-line field support.
 *
 * <p>Verifies:
 * <ul>
 *   <li>Load extracts from {@code offset} to end of line verbatim (no padding removal).</li>
 *   <li>Export writes the value verbatim with no padding or truncation.</li>
 *   <li>Round-trip {@code load(export(x)) == x} holds.</li>
 *   <li>Empty rest-of-line (offset at end of line) yields {@code ""}, not {@code null}.</li>
 *   <li>Offset beyond line end yields {@code null}.</li>
 *   <li>Records with a fixed prefix and a rest-of-line tail work correctly.</li>
 *   <li>Full round-trip file editing: known and unknown lines preserved verbatim.</li>
 *   <li>All invalid configurations are rejected at validation time with clear messages.</li>
 * </ul>
 *
 * @since 1.8.0
 */
public class TestIssue97RestOfLine {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  // ---------------------------------------------------------------------------
  // Load — basic rest-of-line capture
  // ---------------------------------------------------------------------------

  @Test
  void load_restOfLineFromOffset1_capturesEntireLine() {
    FullLineRecord loaded = manager.load(FullLineRecord.class, "Hello World");
    assertEquals("Hello World", loaded.getRawLine());
  }

  @Test
  void load_restOfLineAfterFixedPrefix_capturesSuffix() {
    PrefixAndTailRecord loaded = manager.load(PrefixAndTailRecord.class, "HDR001 free text here");
    assertEquals("HDR", loaded.getType());
    assertEquals("001 free text here", loaded.getTail());
  }

  @Test
  void load_restOfLine_preservesLeadingAndTrailingSpaces() {
    FullLineRecord loaded = manager.load(FullLineRecord.class, "  padded  ");
    assertEquals("  padded  ", loaded.getRawLine());
  }

  @Test
  void load_restOfLine_emptyStringWhenOffsetExactlyAtEndOfLine() {
    // "HDR" is 3 chars; tail starts at offset 4 (0-based: 3 == line.length()), so "" is returned
    PrefixAndTailRecord loaded = manager.load(PrefixAndTailRecord.class, "HDR");
    assertEquals("HDR", loaded.getType());
    assertEquals("", loaded.getTail());
  }

  @Test
  void load_restOfLine_nullWhenOffsetBeyondEndOfLine() {
    // "AB" is 2 chars; tail starts at offset 4 (0-based: 3 > 2), so null is returned
    PrefixAndTailRecord loaded = manager.load(PrefixAndTailRecord.class, "AB");
    assertNull(loaded.getTail());
  }

  @Test
  void load_restOfLineFromOffset1_emptyLineYieldsEmptyString() {
    // empty line: offset 0 == line.length() 0, substring(0) = ""
    FullLineRecord loaded = manager.load(FullLineRecord.class, "");
    assertEquals("", loaded.getRawLine());
  }

  // ---------------------------------------------------------------------------
  // Export — verbatim write, no padding
  // ---------------------------------------------------------------------------

  @Test
  void export_restOfLine_writesVerbatim() {
    FullLineRecord record = new FullLineRecord();
    record.setRawLine("DTL  123  some data");
    assertEquals("DTL  123  some data", manager.export(record));
  }

  @Test
  void export_restOfLine_nullValueExportsAsEmpty() {
    FullLineRecord record = new FullLineRecord();
    record.setRawLine(null);
    assertEquals("", manager.export(record));
  }

  @Test
  void export_prefixAndTail_assemblesCorrectly() {
    PrefixAndTailRecord record = new PrefixAndTailRecord();
    record.setType("HDR");
    record.setTail("001 free text here");
    assertEquals("HDR001 free text here", manager.export(record));
  }

  // ---------------------------------------------------------------------------
  // Round-trip
  // ---------------------------------------------------------------------------

  @Test
  void roundTrip_fullLine_preservesValueExactly() {
    String line = "DTL  123  some data  ";
    FullLineRecord loaded = manager.load(FullLineRecord.class, line);
    String exported = manager.export(loaded);
    assertEquals(line, exported);
  }

  @Test
  void roundTrip_prefixAndTail_preservesValueExactly() {
    String line = "HDR001 free text here";
    PrefixAndTailRecord loaded = manager.load(PrefixAndTailRecord.class, line);
    String exported = manager.export(loaded);
    assertEquals(line, exported);
  }

  @Test
  void roundTrip_emptyTail_preservesEmptyTail() {
    String line = "HDR";
    PrefixAndTailRecord loaded = manager.load(PrefixAndTailRecord.class, line);
    String exported = manager.export(loaded);
    assertEquals(line, exported);
  }

  // ---------------------------------------------------------------------------
  // Integration — heterogeneous file round-trip with UnknownRecord catch-all
  //
  // Known records use simple 3-char fields so export == load (exact length).
  // Unknown lines are captured verbatim and exported verbatim.
  // ---------------------------------------------------------------------------

  @Test
  void roundTrip_heterogeneousFile_allLinesPreservedVerbatim() {
    // Known lines are exactly 3 chars so HeaderRecord/DetailRecord round-trip cleanly.
    // Unknown lines can be any length and are preserved byte-for-byte via UnknownRecord.
    String file = "HDR\nDTL\nunrecognized line here\nDTL\nsome other unknown line";

    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(HeaderRecord.class, LinePattern.prefix("HDR"))
        .addMapping(DetailRecord.class, LinePattern.prefix("DTL"))
        .addMapping(UnknownRecord.class, LinePattern.matchAll())
        .build();

    ReadResult result = reader.read(new StringReader(file));
    List<Object> all = result.getAll();

    assertEquals(5, all.size());
    assertInstanceOf(HeaderRecord.class, all.get(0));
    assertInstanceOf(DetailRecord.class, all.get(1));
    assertInstanceOf(UnknownRecord.class, all.get(2));
    assertInstanceOf(DetailRecord.class, all.get(3));
    assertInstanceOf(UnknownRecord.class, all.get(4));

    // Reconstruct the file from parsed records
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < all.size(); i++) {
      if (i > 0) out.append("\n");
      Object rec = all.get(i);
      if (rec instanceof UnknownRecord) {
        out.append(((UnknownRecord) rec).getRawLine());
      } else {
        out.append(manager.export(rec));
      }
    }

    assertEquals(file, out.toString());
  }

  @Test
  void roundTrip_unknownLinesCapture_verbatimWithSpaces() {
    String unknownLine = "  leading and trailing spaces  ";
    UnknownRecord loaded = manager.load(UnknownRecord.class, unknownLine);
    assertEquals(unknownLine, loaded.getRawLine());
    assertEquals(unknownLine, manager.export(loaded));
  }

  // ---------------------------------------------------------------------------
  // Validation — invalid configurations must throw at load/export time
  // ---------------------------------------------------------------------------

  @Test
  void validate_restOfLine_nonStringType_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(NonStringRestOfLineRecord.class, "12345"));
  }

  @Test
  void validate_restOfLine_countGreaterThanOne_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(CountRestOfLineRecord.class, "abc"));
  }

  @Test
  void validate_restOfLine_explicitAlign_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(AlignRestOfLineRecord.class, "abc"));
  }

  @Test
  void validate_restOfLine_explicitPaddingChar_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(PaddingCharRestOfLineRecord.class, "abc"));
  }

  @Test
  void validate_restOfLine_nullCharSet_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(NullCharRestOfLineRecord.class, "abc"));
  }

  @Test
  void validate_restOfLine_notLastField_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(NotLastFieldRecord.class, "abc123"));
  }

  @Test
  void validate_restOfLine_multipleRestOfLineFields_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class,
        () -> manager.load(MultipleRestOfLineRecord.class, "abc123"));
  }

  @Test
  void validate_repeatingField_effectiveRangeOverlapsRestOfLine_throwsFixedFormatException() {
    // items field: offset=1, length=5, count=3 → effective end offset = 1 + 3*5 - 1 = 15
    // REST_OF_LINE at offset=6 falls inside that range
    assertThrows(FixedFormatException.class,
        () -> manager.load(RepeatingOverlapRecord.class, "something"));
  }

  @Test
  void validate_repeatingField_restOfLineInsideLastElement_throwsFixedFormatException() {
    // items field: offset=1, length=5, count=3 → last element occupies offsets 11–15
    // REST_OF_LINE at offset=13 starts inside that last element.
    // The old formula (start of last element = 1+(3-1)*5 = 11) recorded 11 < 13 → incorrectly passed.
    // The correct formula (end of last element = 1+3*5-1 = 15) records 15 >= 13 → correctly rejects.
    assertThrows(FixedFormatException.class,
        () -> manager.load(RepeatingInnerOverlapRecord.class, "something"));
  }

  @Test
  void validate_singleField_restOfLineInsideFieldRange_throwsFixedFormatException() {
    // @Field(offset=1, length=10) occupies bytes 1–10; REST_OF_LINE at offset=5 is inside it.
    // effectiveEndOffset = 1 + 10 - 1 = 10, which is >= 5 → correctly rejected.
    assertThrows(FixedFormatException.class,
        () -> manager.load(OverlappingSingleFieldRecord.class, "data"));
  }

  @Test
  void validate_restOfLine_withExplicitRecordLength_throwsFixedFormatException() {
    // @Record(length = 10) causes padding after the REST_OF_LINE field on export,
    // silently corrupting the verbatim round-trip — must be rejected at validation time
    assertThrows(FixedFormatException.class,
        () -> manager.load(FixedLengthWithRestOfLineRecord.class, "abc"));
  }

  // ---------------------------------------------------------------------------
  // Record definitions — valid
  // ---------------------------------------------------------------------------

  @Record
  public static class FullLineRecord {
    private String rawLine;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
  }

  @Record
  public static class PrefixAndTailRecord {
    private String type;
    private String tail;

    @Field(offset = 1, length = 3)
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    @Field(offset = 4, length = Field.REST_OF_LINE)
    public String getTail() { return tail; }
    public void setTail(String tail) { this.tail = tail; }
  }

  @Record(length = 3)
  public static class HeaderRecord {
    private String type;

    @Field(offset = 1, length = 3)
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
  }

  @Record(length = 3)
  public static class DetailRecord {
    private String type;

    @Field(offset = 1, length = 3)
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
  }

  @Record
  public static class UnknownRecord {
    private String rawLine;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
  }

  // ---------------------------------------------------------------------------
  // Record definitions — invalid (trigger validation errors)
  // ---------------------------------------------------------------------------

  @Record
  public static class NonStringRestOfLineRecord {
    private Integer value;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public Integer getValue() { return value; }
    public void setValue(Integer value) { this.value = value; }
  }

  @Record
  public static class CountRestOfLineRecord {
    private List<String> values;

    @Field(offset = 1, length = Field.REST_OF_LINE, count = 2)
    public List<String> getValues() { return values; }
    public void setValues(List<String> values) { this.values = values; }
  }

  @Record
  public static class AlignRestOfLineRecord {
    private String rawLine;

    @Field(offset = 1, length = Field.REST_OF_LINE, align = Align.RIGHT)
    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
  }

  @Record
  public static class PaddingCharRestOfLineRecord {
    private String rawLine;

    @Field(offset = 1, length = Field.REST_OF_LINE, paddingChar = '*')
    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
  }

  @Record
  public static class NullCharRestOfLineRecord {
    private String rawLine;

    @Field(offset = 1, length = Field.REST_OF_LINE, nullChar = ' ')
    public String getRawLine() { return rawLine; }
    public void setRawLine(String rawLine) { this.rawLine = rawLine; }
  }

  @Record
  public static class NotLastFieldRecord {
    private String tail;
    private String fixed;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public String getTail() { return tail; }
    public void setTail(String tail) { this.tail = tail; }

    @Field(offset = 4, length = 3)
    public String getFixed() { return fixed; }
    public void setFixed(String fixed) { this.fixed = fixed; }
  }

  @Record
  public static class MultipleRestOfLineRecord {
    private String first;
    private String second;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public String getFirst() { return first; }
    public void setFirst(String first) { this.first = first; }

    @Field(offset = 2, length = Field.REST_OF_LINE)
    public String getSecond() { return second; }
    public void setSecond(String second) { this.second = second; }
  }

  @Record
  public static class RepeatingOverlapRecord {
    private List<String> items;
    private String tail;

    @Field(offset = 1, length = 5, count = 3)
    public List<String> getItems() { return items; }
    public void setItems(List<String> items) { this.items = items; }

    @Field(offset = 6, length = Field.REST_OF_LINE)
    public String getTail() { return tail; }
    public void setTail(String tail) { this.tail = tail; }
  }

  @Record(length = 10)
  public static class FixedLengthWithRestOfLineRecord {
    private String tail;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public String getTail() { return tail; }
    public void setTail(String tail) { this.tail = tail; }
  }

  @Record
  public static class OverlappingSingleFieldRecord {
    private String prefix;
    private String tail;

    @Field(offset = 1, length = 10)
    public String getPrefix() { return prefix; }
    public void setPrefix(String prefix) { this.prefix = prefix; }

    // offset=5 falls inside the prefix field (bytes 1–10)
    @Field(offset = 5, length = Field.REST_OF_LINE)
    public String getTail() { return tail; }
    public void setTail(String tail) { this.tail = tail; }
  }

  @Record
  public static class RepeatingInnerOverlapRecord {
    private List<String> items;
    private String tail;

    @Field(offset = 1, length = 5, count = 3)
    public List<String> getItems() { return items; }
    public void setItems(List<String> items) { this.items = items; }

    // offset=13 starts inside the last element (offsets 11–15) of the repeating field above
    @Field(offset = 13, length = Field.REST_OF_LINE)
    public String getTail() { return tail; }
    public void setTail(String tail) { this.tail = tail; }
  }
}
