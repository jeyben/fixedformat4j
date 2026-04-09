package com.ancientprogramming.fixedformat4j.format.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

public class TestRepeatingFieldSupport {

  private final RepeatingFieldSupport support = new RepeatingFieldSupport();

  // =========================================================================
  // validateCountAnnotation
  // =========================================================================

  @Test
  void validateCount_countLessThanOne_throwsFixedFormatException() throws Exception {
    Method getter = ValidationFixtures.class.getMethod("getNegativeCount");
    Field fieldAnno = getter.getAnnotation(Field.class);
    assertThrows(FixedFormatException.class, () -> support.validateCount(getter, fieldAnno));
  }

  @Test
  void validateCount_countOneOnCollection_throwsFixedFormatException() throws Exception {
    Method getter = ValidationFixtures.class.getMethod("getCountOneList");
    Field fieldAnno = getter.getAnnotation(Field.class);
    assertThrows(FixedFormatException.class, () -> support.validateCount(getter, fieldAnno));
  }

  @Test
  void validateCount_countGreaterOneOnScalar_throwsFixedFormatException() throws Exception {
    Method getter = ValidationFixtures.class.getMethod("getCountManyScalar");
    Field fieldAnno = getter.getAnnotation(Field.class);
    assertThrows(FixedFormatException.class, () -> support.validateCount(getter, fieldAnno));
  }

  @Test
  void validateCount_validCountOnCollection_doesNotThrow() throws Exception {
    Method getter = ValidationFixtures.class.getMethod("getValidList");
    Field fieldAnno = getter.getAnnotation(Field.class);
    assertDoesNotThrow(() -> support.validateCount(getter, fieldAnno));
  }

  // =========================================================================
  // resolveElementType
  // =========================================================================

  @Test
  void resolveElementType_array_returnsComponentType() throws Exception {
    Method getter = RepeatingFieldRecord.class.getMethod("getCodes");
    assertEquals(String.class, support.resolveElementType(getter));
  }

  @Test
  void resolveElementType_parameterizedList_returnsTypeArg() throws Exception {
    Method getter = RepeatingFieldCollectionRecord.class.getMethod("getCodes");
    assertEquals(String.class, support.resolveElementType(getter));
  }

  @Test
  @SuppressWarnings("rawtypes")
  void resolveElementType_rawCollection_throwsFixedFormatException() throws Exception {
    Method getter = ValidationFixtures.class.getMethod("getRawCollection");
    assertThrows(FixedFormatException.class, () -> support.resolveElementType(getter));
  }

  // =========================================================================
  // assembleCollection
  // =========================================================================

  @Test
  void assembleCollection_array_returnsCorrectArray() throws Exception {
    Method getter = RepeatingFieldRecord.class.getMethod("getCodes");
    Object result = support.assembleCollection(getter, Arrays.asList("a", "b", "c"));
    assertInstanceOf(String[].class, result);
    assertArrayEquals(new String[]{"a", "b", "c"}, (String[]) result);
  }

  @Test
  void assembleCollection_list_returnsArrayList() throws Exception {
    Method getter = RepeatingFieldCollectionRecord.class.getMethod("getCodes");
    Object result = support.assembleCollection(getter, Arrays.asList("a", "b"));
    assertInstanceOf(List.class, result);
    assertEquals(Arrays.asList("a", "b"), result);
  }

  @Test
  void assembleCollection_linkedList_returnsLinkedList() throws Exception {
    Method getter = ValidationFixtures.class.getMethod("getLinkedList");
    Object result = support.assembleCollection(getter, Arrays.asList("x", "y"));
    assertInstanceOf(LinkedList.class, result);
  }

  @Test
  void assembleCollection_sortedSet_returnsTreeSet() throws Exception {
    Method getter = ValidationFixtures.class.getMethod("getSortedSet");
    Object result = support.assembleCollection(getter, Arrays.asList("b", "a"));
    assertInstanceOf(TreeSet.class, result);
  }

  @Test
  void assembleCollection_set_returnsLinkedHashSet() throws Exception {
    Method getter = ValidationFixtures.class.getMethod("getSet");
    Object result = support.assembleCollection(getter, Arrays.asList("x"));
    assertInstanceOf(Set.class, result);
  }

  @Test
  void assembleCollection_unsupportedType_throwsFixedFormatException() throws Exception {
    Method getter = ValidationFixtures.class.getMethod("getString");
    assertThrows(FixedFormatException.class,
        () -> support.assembleCollection(getter, Arrays.asList("a", "b")));
  }

  // =========================================================================
  // read
  // =========================================================================

  @Test
  void read_repeatingField_parsesAllElements() throws Exception {
    Method getter = RepeatingFieldRecord.class.getMethod("getCodes");
    Field fieldAnno = getter.getAnnotation(Field.class);
    String data = "ab   cd   ef   0004200099";

    Object result = support.read(RepeatingFieldRecord.class, data, getter, getter, fieldAnno);

    assertInstanceOf(String[].class, result);
    String[] codes = (String[]) result;
    assertEquals(3, codes.length);
    assertEquals("ab", codes[0]);
    assertEquals("cd", codes[1]);
    assertEquals("ef", codes[2]);
  }

  // =========================================================================
  // export
  // =========================================================================

  @Test
  void export_repeatingField_writesAllElements() throws Exception {
    RepeatingFieldRecord record = new RepeatingFieldRecord();
    record.setCodes(new String[]{"ab", "cd", "ef"});
    record.setAmounts(new Integer[]{42, 99});

    Method getter = RepeatingFieldRecord.class.getMethod("getCodes");
    Field fieldAnno = getter.getAnnotation(Field.class);
    AnnotationTarget target = AnnotationTarget.ofMethod(getter);
    HashMap<Integer, String> foundData = new HashMap<>();

    support.export(record, target, fieldAnno, foundData);

    assertEquals(3, foundData.size());
    assertEquals("ab   ", foundData.get(1));
    assertEquals("cd   ", foundData.get(6));
    assertEquals("ef   ", foundData.get(11));
  }

  @Test
  void export_nullCollection_throwsFixedFormatException() throws Exception {
    RepeatingFieldRecord record = new RepeatingFieldRecord();
    record.setCodes(null);

    Method getter = RepeatingFieldRecord.class.getMethod("getCodes");
    Field fieldAnno = getter.getAnnotation(Field.class);
    AnnotationTarget target = AnnotationTarget.ofMethod(getter);

    assertThrows(FixedFormatException.class,
        () -> support.export(record, target, fieldAnno, new HashMap<>()));
  }

  @Test
  void export_sizeMismatch_strictMode_throwsFixedFormatException() throws Exception {
    RepeatingFieldRecord record = new RepeatingFieldRecord();
    record.setCodes(new String[]{"ab", "cd"});  // count=3 but only 2 elements

    Method getter = RepeatingFieldRecord.class.getMethod("getCodes");
    Field fieldAnno = getter.getAnnotation(Field.class);
    AnnotationTarget target = AnnotationTarget.ofMethod(getter);

    assertThrows(FixedFormatException.class,
        () -> support.export(record, target, fieldAnno, new HashMap<>()));
  }

  @Test
  void export_sizeMismatch_nonStrictMode_logsWarnAndExportsMin() throws Exception {
    Method getter = LenientFixture.class.getMethod("getCodes");
    Field fieldAnno = getter.getAnnotation(Field.class);

    LenientFixture record = new LenientFixture();
    record.setCodes(new String[]{"ab", "cd"});  // count=3 but only 2 elements

    AnnotationTarget target = AnnotationTarget.ofMethod(getter);
    HashMap<Integer, String> foundData = new HashMap<>();

    Logger logger = (Logger) LoggerFactory.getLogger(RepeatingFieldSupport.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.WARN);
    try {
      support.export(record, target, fieldAnno, foundData);
    } finally {
      logger.detachAppender(appender);
    }

    assertEquals(2, foundData.size());
    assertTrue(appender.list.stream()
        .anyMatch(e -> e.getLevel() == Level.WARN && e.getFormattedMessage().contains("getCodes")),
        "Expected WARN log mentioning getCodes");
  }

  // =========================================================================
  // Fixture classes
  // =========================================================================

  @Record
  public static class ValidationFixtures {
    @Field(offset = 1, length = 5, count = -1)
    public String getNegativeCount() { return null; }

    @Field(offset = 1, length = 5, count = 1)
    public List<String> getCountOneList() { return null; }

    @Field(offset = 1, length = 5, count = 3)
    public String getCountManyScalar() { return null; }

    @Field(offset = 1, length = 5, count = 2)
    public List<String> getValidList() { return null; }

    @Field(offset = 1, length = 5, count = 2)
    @SuppressWarnings("rawtypes")
    public Collection getRawCollection() { return null; }

    @Field(offset = 1, length = 5, count = 2)
    public LinkedList<String> getLinkedList() { return null; }

    @Field(offset = 1, length = 5, count = 2)
    public SortedSet<String> getSortedSet() { return null; }

    @Field(offset = 1, length = 5, count = 2)
    public Set<String> getSet() { return null; }

    @Field(offset = 1, length = 5)
    public String getString() { return null; }
  }

  @Record
  public static class LenientFixture {
    private String[] codes;

    @Field(offset = 1, length = 5, count = 3, strictCount = false)
    public String[] getCodes() { return codes; }
    public void setCodes(String[] codes) { this.codes = codes; }
  }
}
