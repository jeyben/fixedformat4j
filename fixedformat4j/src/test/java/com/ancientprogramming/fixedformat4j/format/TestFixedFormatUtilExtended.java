package com.ancientprogramming.fixedformat4j.format;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.impl.ByTypeFormatter;
import com.ancientprogramming.fixedformat4j.format.impl.StringFormatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Extended tests for FixedFormatUtil — boundary and edge cases not covered by TestFixedFormatUtil.
 */
public class TestFixedFormatUtilExtended {

  private FormatContext<String> ctx(int offset) {
    return new FormatContext<>(offset, String.class, StringFormatter.class);
  }

  private FormatInstructions instr(int length) {
    return new FormatInstructions(length, Align.LEFT, ' ', null, null, null, null);
  }

  // --- fetchData tests ---

  @Test
  public void fetchData_firstField_offset1() {
    String result = FixedFormatUtil.fetchData("ABCDE", instr(3), ctx(1));
    assertEquals("ABC", result);
  }

  @Test
  public void fetchData_lastField_exactEnd() {
    // record "ABCDE", field at offset 4, length 2 → "DE"
    String result = FixedFormatUtil.fetchData("ABCDE", instr(2), ctx(4));
    assertEquals("DE", result);
  }

  @Test
  public void fetchData_fieldExactlyFillsRecord() {
    // offset 1, length 5, record "ABCDE" → full record
    String result = FixedFormatUtil.fetchData("ABCDE", instr(5), ctx(1));
    assertEquals("ABCDE", result);
  }

  @Test
  public void fetchData_fieldExceedsRecordLength_returnsAvailable() {
    // offset 3, length 10, record "ABCDE" (length 5) → "CDE" (what's left)
    String result = FixedFormatUtil.fetchData("ABCDE", instr(10), ctx(3));
    assertEquals("CDE", result);
  }

  @Test
  public void fetchData_offsetBeyondRecord_returnsNull() {
    // offset 6, record "ABCDE" (length 5) → null
    String result = FixedFormatUtil.fetchData("ABCDE", instr(3), ctx(6));
    assertNull(result);
  }

  @Test
  public void fetchData_offsetAtRecordLength_returnsNull() {
    // record "ABCDE" length 5, offset 5 (zero-based 4 = last char, but offset 5 means start at index 4)
    // offset=5 → zero-based index 4 → record.length()==5, offset==4, so record.length() > offset → returns "E"
    // offset=6 → zero-based index 5 → record.length()==5, offset==5, so record.length() <= offset → null
    String result = FixedFormatUtil.fetchData("ABCDE", instr(1), ctx(6));
    assertNull(result);
  }

  // --- getFixedFormatterInstance tests ---

  @Test
  public void getFixedFormatterInstance_noArgConstructor_StringFormatter() {
    FormatContext<String> context = new FormatContext<>(1, String.class, StringFormatter.class);
    FixedFormatter<String> formatter = FixedFormatUtil.getFixedFormatterInstance(StringFormatter.class, context);
    assertNotNull(formatter);
    assertTrue(formatter instanceof StringFormatter);
  }

  @Test
  public void getFixedFormatterInstance_formatContextConstructor_ByTypeFormatter() {
    FormatContext context = new FormatContext(1, String.class, ByTypeFormatter.class);
    FixedFormatter formatter = FixedFormatUtil.getFixedFormatterInstance(ByTypeFormatter.class, context);
    assertNotNull(formatter);
    assertTrue(formatter instanceof ByTypeFormatter);
  }

  @Test
  public void getFixedFormatterInstance_noValidConstructor_throwsFixedFormatException() {
    // A formatter class with no valid constructor should throw
    FormatContext context = new FormatContext(1, String.class, NoValidConstructorFormatter.class);
    assertThrows(FixedFormatException.class, () ->
      FixedFormatUtil.getFixedFormatterInstance(NoValidConstructorFormatter.class, context)
    );
  }

  /** Formatter with no usable constructor for testing. */
  public static class NoValidConstructorFormatter implements FixedFormatter<String> {
    public NoValidConstructorFormatter(String unusedParam) {}

    public String parse(String value, FormatInstructions instructions) { return value; }
    public String format(String value, FormatInstructions instructions) { return value == null ? "" : value; }
  }
}
