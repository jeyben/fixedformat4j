package com.ancientprogramming.fixedformat4j.format.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for @Field count — repeating fields mapped to arrays and collections.
 *
 * <p>String fields use LEFT alignment with space padding (default), so trailing spaces are
 * stripped on load. Test values are stored as trimmed strings; export re-pads them.</p>
 */
public class TestRepeatingField {

  // -----------------------------------------------------------------------
  // Record data constants
  //
  // RepeatingFieldRecord layout (1-based):
  //   positions  1- 5 : codes[0] — String, length 5, LEFT aligned, space-padded
  //   positions  6-10 : codes[1]
  //   positions 11-15 : codes[2]
  //   positions 16-20 : amounts[0] — Integer, length 5, RIGHT aligned, zero-padded
  //   positions 21-25 : amounts[1]
  //
  // Loaded string values are trimmed (trailing padding chars stripped by LEFT.remove).
  // -----------------------------------------------------------------------
  private static final String ARRAY_RECORD_DATA = "ab   cd   ef   0004200099";

  // RepeatingFieldCollectionRecord layout (1-based):
  //   positions  1-15 : 3 × String[5] codes
  //   positions 16-25 : 2 × Integer[5] amounts (RIGHT, zero-padded)
  //   positions 26-29 : tags[0] — String, length 4
  //   positions 30-33 : tags[1]
  //   positions 34-37 : tags[2]
  //
  // NOTE: no spaces between tag slots — contiguous 4-char fields.
  private static final String COLLECTION_RECORD_DATA = "ab   cd   ef   0004200099aaaabbbbcccc";

  private FixedFormatManager manager;

  @BeforeEach
  public void setUp() {
    manager = new FixedFormatManagerImpl();
  }

  // =========================================================================
  // Static nested record fixtures for validation-error tests
  // (Static nested classes can be instantiated by FixedFormatManagerImpl
  //  without an enclosing instance, unlike local classes in test methods.)
  // =========================================================================

  @Record
  static class CountNegativeRecord {
    private String value;
    @Field(offset = 1, length = 5, count = -1)
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
  }

  @Record
  static class CountZeroRecord {
    private String value;
    @Field(offset = 1, length = 5, count = 0)
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
  }

  @Record
  static class CountOneArrayRecord {
    private String[] values;
    @Field(offset = 1, length = 5) // count=1 default, but return type is array
    public String[] getValues() { return values; }
    public void setValues(String[] v) { this.values = v; }
  }

  @Record
  static class CountManyScalarRecord {
    private String value;
    @Field(offset = 1, length = 5, count = 3)
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
  }

  // -----------------------------------------------------------------------
  // Lenient export fixtures (strictExportCount = false)
  // -----------------------------------------------------------------------

  @Record
  static class LenientRecord {
    private String[] codes;
    private Integer[] amounts;

    @Field(offset = 1, length = 5, count = 3, strictExportCount = false)
    public String[] getCodes() { return codes; }
    public void setCodes(String[] codes) { this.codes = codes; }

    @Field(offset = 16, length = 5, count = 2, align = Align.RIGHT, paddingChar = '0', strictExportCount = false)
    public Integer[] getAmounts() { return amounts; }
    public void setAmounts(Integer[] amounts) { this.amounts = amounts; }
  }

  @Record
  static class LenientListRecord {
    private List<String> codes;

    @Field(offset = 1, length = 5, count = 3, strictExportCount = false)
    public List<String> getCodes() { return codes; }
    public void setCodes(List<String> codes) { this.codes = codes; }
  }

  // =========================================================================
  // Sunshine — arrays
  // =========================================================================

  @Test
  public void testLoadStringArray() {
    RepeatingFieldRecord record = manager.load(RepeatingFieldRecord.class, ARRAY_RECORD_DATA);
    assertNotNull(record.getCodes());
    assertEquals(3, record.getCodes().length);
    // LEFT-aligned strings have trailing padding stripped on load
    assertEquals("ab", record.getCodes()[0]);
    assertEquals("cd", record.getCodes()[1]);
    assertEquals("ef", record.getCodes()[2]);
  }

  @Test
  public void testLoadIntegerArray() {
    RepeatingFieldRecord record = manager.load(RepeatingFieldRecord.class, ARRAY_RECORD_DATA);
    assertNotNull(record.getAmounts());
    assertEquals(2, record.getAmounts().length);
    assertEquals(42, record.getAmounts()[0]);
    assertEquals(99, record.getAmounts()[1]);
  }

  @Test
  public void testExportStringArray() {
    RepeatingFieldRecord record = new RepeatingFieldRecord();
    // Formatter pads "ab" to "ab   " (length 5, LEFT aligned)
    record.setCodes(new String[]{"ab", "cd", "ef"});
    record.setAmounts(new Integer[]{42, 99});
    assertEquals(ARRAY_RECORD_DATA, manager.export(record));
  }

  @Test
  public void testExportIntegerArray() {
    RepeatingFieldRecord record = new RepeatingFieldRecord();
    record.setCodes(new String[]{"xx", "yy", "zz"});
    record.setAmounts(new Integer[]{1, 2});
    String exported = manager.export(record);
    // positions 16-20 (0-based 15-19) = "00001", positions 21-25 (0-based 20-24) = "00002"
    assertEquals("00001", exported.substring(15, 20));
    assertEquals("00002", exported.substring(20, 25));
  }

  @Test
  public void testRoundTripStringArray() {
    RepeatingFieldRecord original = new RepeatingFieldRecord();
    original.setCodes(new String[]{"abc", "def", "ghi"});
    original.setAmounts(new Integer[]{100, 200});
    String exported = manager.export(original);
    RepeatingFieldRecord loaded = manager.load(RepeatingFieldRecord.class, exported);
    assertArrayEquals(original.getCodes(), loaded.getCodes());
    assertArrayEquals(original.getAmounts(), loaded.getAmounts());
  }

  @Test
  public void testRoundTripIntegerArray() {
    RepeatingFieldRecord original = new RepeatingFieldRecord();
    original.setCodes(new String[]{"a", "b", "c"});
    original.setAmounts(new Integer[]{12345, 67890});
    String exported = manager.export(original);
    RepeatingFieldRecord loaded = manager.load(RepeatingFieldRecord.class, exported);
    assertArrayEquals(original.getAmounts(), loaded.getAmounts());
  }

  // =========================================================================
  // Sunshine — collections
  // =========================================================================

  @Test
  public void testLoadStringList() {
    RepeatingFieldCollectionRecord record = manager.load(RepeatingFieldCollectionRecord.class, COLLECTION_RECORD_DATA);
    assertNotNull(record.getCodes());
    assertEquals(3, record.getCodes().size());
    // Trailing padding stripped by LEFT.remove
    assertEquals("ab", record.getCodes().get(0));
    assertEquals("cd", record.getCodes().get(1));
    assertEquals("ef", record.getCodes().get(2));
  }

  @Test
  public void testLoadIntegerCollection() {
    RepeatingFieldCollectionRecord record = manager.load(RepeatingFieldCollectionRecord.class, COLLECTION_RECORD_DATA);
    assertNotNull(record.getAmounts());
    assertEquals(2, record.getAmounts().size());
    List<Integer> amounts = new java.util.ArrayList<>(record.getAmounts());
    assertEquals(42, amounts.get(0));
    assertEquals(99, amounts.get(1));
  }

  @Test
  public void testLoadStringLinkedHashSet() {
    RepeatingFieldCollectionRecord record = manager.load(RepeatingFieldCollectionRecord.class, COLLECTION_RECORD_DATA);
    assertNotNull(record.getTags());
    assertEquals(3, record.getTags().size());
    assertTrue(record.getTags() instanceof LinkedHashSet, "Expected LinkedHashSet");
    // Insertion order preserved — tags are exact 4-char fields with no trailing spaces
    List<String> tags = new java.util.ArrayList<>(record.getTags());
    assertEquals("aaaa", tags.get(0));
    assertEquals("bbbb", tags.get(1));
    assertEquals("cccc", tags.get(2));
  }

  @Test
  public void testExportStringList() {
    RepeatingFieldCollectionRecord record = new RepeatingFieldCollectionRecord();
    record.setCodes(Arrays.asList("ab", "cd", "ef"));
    record.setAmounts(Arrays.asList(42, 99));
    record.setTags(new LinkedHashSet<>(Arrays.asList("aaaa", "bbbb", "cccc")));
    assertEquals(COLLECTION_RECORD_DATA, manager.export(record));
  }

  @Test
  public void testExportLinkedHashSet() {
    RepeatingFieldCollectionRecord record = new RepeatingFieldCollectionRecord();
    record.setCodes(Arrays.asList("a", "b", "c"));
    record.setAmounts(Arrays.asList(0, 0));
    Set<String> tags = new LinkedHashSet<>(Arrays.asList("xxxx", "yyyy", "zzzz"));
    record.setTags(tags);
    String exported = manager.export(record);
    // positions 26-29 (0-based 25-28) = "xxxx"
    assertEquals("xxxx", exported.substring(25, 29));
    assertEquals("yyyy", exported.substring(29, 33));
    assertEquals("zzzz", exported.substring(33, 37));
  }

  @Test
  public void testRoundTripStringList() {
    RepeatingFieldCollectionRecord original = new RepeatingFieldCollectionRecord();
    original.setCodes(Arrays.asList("abc", "def", "ghi"));
    original.setAmounts(Arrays.asList(11, 22));
    original.setTags(new LinkedHashSet<>(Arrays.asList("t1  ", "t2  ", "t3  ")));
    String exported = manager.export(original);
    RepeatingFieldCollectionRecord loaded = manager.load(RepeatingFieldCollectionRecord.class, exported);
    assertEquals(original.getCodes(), loaded.getCodes());
    assertEquals(new java.util.ArrayList<>(original.getAmounts()), new java.util.ArrayList<>(loaded.getAmounts()));
  }

  @Test
  public void testRoundTripLinkedHashSet() {
    RepeatingFieldCollectionRecord original = new RepeatingFieldCollectionRecord();
    original.setCodes(Arrays.asList("a", "b", "c"));
    original.setAmounts(Arrays.asList(0, 0));
    // Use values with no trailing spaces so round-trip is exact after LEFT-strip
    original.setTags(new LinkedHashSet<>(Arrays.asList("aaa", "bbb", "ccc")));
    String exported = manager.export(original);
    RepeatingFieldCollectionRecord loaded = manager.load(RepeatingFieldCollectionRecord.class, exported);
    // Loaded as LinkedHashSet — order and values must match
    assertEquals(new java.util.ArrayList<>(original.getTags()), new java.util.ArrayList<>(loaded.getTags()));
  }

  // =========================================================================
  // Sunshine — backward compat (count=1, default)
  // =========================================================================

  @Test
  public void testCountOneBackwardCompat() {
    String data = "some text 0012320080514CT001100000010350000002056-0012 01200000002056";
    MyRecord record = manager.load(MyRecord.class, data);
    assertNotNull(record);
    assertEquals(123, record.getIntegerData());
  }

  // =========================================================================
  // Corner / validation — error messages must contain class + method name
  // =========================================================================

  @Test
  public void testNegativeCountThrows() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(CountNegativeRecord.class, "hello"));
    assertTrue(ex.getMessage().contains("getValue"), "Error must contain method name, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("count"), "Error must mention count, got: " + ex.getMessage());
  }

  @Test
  public void testCountZeroThrows() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(CountZeroRecord.class, "hello"));
    assertTrue(ex.getMessage().contains("getValue"), "Error must contain method name, got: " + ex.getMessage());
  }

  @Test
  public void testCountOneWithArrayTypeThrows() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(CountOneArrayRecord.class, "hello"));
    assertTrue(ex.getMessage().contains("getValues"), "Error must contain method name, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("count=1"), "Error must mention count=1, got: " + ex.getMessage());
  }

  @Test
  public void testCountManyWithScalarTypeThrows() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(CountManyScalarRecord.class, "helloworld     "));
    assertTrue(ex.getMessage().contains("getValue"), "Error must contain method name, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("count=3"), "Error must mention count=3, got: " + ex.getMessage());
  }

  @Test
  public void testExportNullArrayThrows() {
    RepeatingFieldRecord record = new RepeatingFieldRecord();
    record.setCodes(null);
    record.setAmounts(new Integer[]{1, 2});
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.export(record));
    assertTrue(ex.getMessage().contains("getCodes"), "Error must contain method name, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("null"), "Error must mention null, got: " + ex.getMessage());
  }

  @Test
  public void testExportNullListThrows() {
    RepeatingFieldCollectionRecord record = new RepeatingFieldCollectionRecord();
    record.setCodes(null);
    record.setAmounts(Arrays.asList(1, 2));
    record.setTags(new LinkedHashSet<>(Arrays.asList("a", "b", "c")));
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.export(record));
    assertTrue(ex.getMessage().contains("getCodes"), "Error must contain method name, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("null"), "Error must mention null, got: " + ex.getMessage());
  }

  // -----------------------------------------------------------------------
  // Strict export count (default = true) — size mismatch must throw
  // -----------------------------------------------------------------------

  @Test
  public void testExportArrayShorterThanCountStrictThrows() {
    RepeatingFieldRecord record = new RepeatingFieldRecord();
    record.setCodes(new String[]{"ab", "cd"}); // 2 elements, count=3
    record.setAmounts(new Integer[]{1, 2});
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.export(record));
    assertTrue(ex.getMessage().contains("getCodes"), "Error must contain method name, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("count=3"), "Error must mention expected count, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("size=2"), "Error must mention actual size, got: " + ex.getMessage());
  }

  @Test
  public void testExportArrayLongerThanCountStrictThrows() {
    RepeatingFieldRecord record = new RepeatingFieldRecord();
    record.setCodes(new String[]{"ab", "cd", "ef", "gh"}); // 4 elements, count=3
    record.setAmounts(new Integer[]{1, 2});
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.export(record));
    assertTrue(ex.getMessage().contains("getCodes"), "Error must contain method name, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("count=3"), "Error must mention expected count, got: " + ex.getMessage());
    assertTrue(ex.getMessage().contains("size=4"), "Error must mention actual size, got: " + ex.getMessage());
  }

  // -----------------------------------------------------------------------
  // Lenient export count (strictExportCount=false) — size mismatch logs WARN
  // -----------------------------------------------------------------------

  private ListAppender<ILoggingEvent> attachLogCapture() {
    Logger logger = (Logger) LoggerFactory.getLogger(FixedFormatManagerImpl.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.WARN);
    return appender;
  }

  private void detachLogCapture(ListAppender<ILoggingEvent> appender) {
    Logger logger = (Logger) LoggerFactory.getLogger(FixedFormatManagerImpl.class);
    logger.detachAppender(appender);
  }

  @Test
  public void testExportArrayShorterThanCountLenientLogsWarn() {
    LenientRecord record = new LenientRecord();
    record.setCodes(new String[]{"ab", "cd"}); // 2 elements, count=3
    record.setAmounts(new Integer[]{1, 2});

    ListAppender<ILoggingEvent> appender = attachLogCapture();
    String exported;
    try {
      exported = manager.export(record);
    } finally {
      detachLogCapture(appender);
    }

    // No exception — available elements are exported
    assertTrue(exported.startsWith("ab   cd   "), "Available elements should be exported, got: " + exported);

    assertTrue(appender.list.stream()
        .anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("getCodes")),
        "A WARN log entry mentioning getCodes must be emitted");
  }

  @Test
  public void testExportArrayLongerThanCountLenientLogsWarn() {
    LenientRecord record = new LenientRecord();
    record.setCodes(new String[]{"ab", "cd", "ef", "gh"}); // 4 elements, count=3
    record.setAmounts(new Integer[]{1, 2});

    ListAppender<ILoggingEvent> appender = attachLogCapture();
    String exported;
    try {
      exported = manager.export(record);
    } finally {
      detachLogCapture(appender);
    }

    // Only first 3 elements exported
    assertEquals("ab   ", exported.substring(0, 5));
    assertEquals("cd   ", exported.substring(5, 10));
    assertEquals("ef   ", exported.substring(10, 15));

    assertTrue(appender.list.stream()
        .anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("getCodes")),
        "A WARN log entry mentioning getCodes must be emitted");
  }

  @Test
  public void testExportListShorterThanCountLenientLogsWarn() {
    LenientListRecord record = new LenientListRecord();
    record.setCodes(Arrays.asList("ab")); // 1 element, count=3

    ListAppender<ILoggingEvent> appender = attachLogCapture();
    String exported;
    try {
      exported = manager.export(record);
    } finally {
      detachLogCapture(appender);
    }

    assertEquals("ab   ", exported.substring(0, 5));

    assertTrue(appender.list.stream()
        .anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("getCodes")),
        "A WARN log entry mentioning getCodes must be emitted");
  }

  // =========================================================================
  // Corner — partial record (record string shorter than last element offset)
  // =========================================================================

  @Test
  public void testLoadPartialRecord() {
    // Only first two code slots provided; third element (positions 11-15) is beyond string length
    // FixedFormatUtil returns null for out-of-bounds; StringFormatter parses null to null
    String shortData = "ab   cd   "; // 10 chars
    RepeatingFieldRecord record = manager.load(RepeatingFieldRecord.class, shortData);
    assertNotNull(record.getCodes());
    assertEquals(3, record.getCodes().length);
    assertEquals("ab", record.getCodes()[0]);
    assertEquals("cd", record.getCodes()[1]);
    assertNull(record.getCodes()[2], "Element beyond record length should be null");
  }
}
