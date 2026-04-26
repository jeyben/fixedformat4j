package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the deprecated {@code readDataAccordingFieldAnnotation} API (Cluster F).
 *
 * <p>The method is {@code protected} on {@link FixedFormatManagerImpl}; tests use an
 * anonymous subclass as the minimal seam to invoke it directly. The main {@code load()} path
 * uses {@link ClassMetadataCache} and does NOT call this method, so existing tests do not
 * cover it — surviving mutants accumulate here.
 *
 * <p>Note: the {@code count != 1} path that delegates to {@link RepeatingFieldSupport} is
 * covered by {@link #readData_repeatingField_returnsArray()}.
 */
@SuppressWarnings("deprecation")
public class TestFixedFormatManagerImplLegacyApi {

  // Same package as FixedFormatManagerImpl — protected method accessible without subclassing.
  private final FixedFormatManagerImpl manager = new FixedFormatManagerImpl();

  // --- F1: Normal single-field parse ---

  @Test
  public void readData_singleStringField_parsesValue() throws Exception {
    Method getter = LegacySimpleRecord.class.getMethod("getText");
    Field fieldAnno = getter.getAnnotation(Field.class);
    Object result = manager.readDataAccordingFieldAnnotation(
        LegacySimpleRecord.class, "hello     ", getter, getter, fieldAnno);
    assertEquals("hello", result,
        "Should parse the string value at the given offset/length");
  }

  @Test
  public void readData_singleIntegerField_parsesValue() throws Exception {
    Method getter = LegacyIntRecord.class.getMethod("getNumber");
    Field fieldAnno = getter.getAnnotation(Field.class);
    Object result = manager.readDataAccordingFieldAnnotation(
        LegacyIntRecord.class, "00042", getter, getter, fieldAnno);
    assertEquals(42, result,
        "Should parse the integer value (with leading-zero padding stripped)");
  }

  // --- F2: Repeating field (count > 1) ---

  @Test
  public void readData_repeatingField_returnsArray() throws Exception {
    Method getter = LegacyRepeatingRecord.class.getMethod("getCodes");
    Field fieldAnno = getter.getAnnotation(Field.class);
    Object result = manager.readDataAccordingFieldAnnotation(
        LegacyRepeatingRecord.class, "AAABBBCCC", getter, getter, fieldAnno);
    assertNotNull(result, "Repeating field result should not be null");
    assertInstanceOf(String[].class, result, "Repeating String field should return String[]");
    String[] codes = (String[]) result;
    assertEquals(3, codes.length);
    assertEquals("AAA", codes[0]);
    assertEquals("BBB", codes[1]);
    assertEquals("CCC", codes[2]);
  }

  // --- F3: Nested @Record type — recursive load ---

  @Test
  public void readData_nestedRecordField_recursivelyLoadsInnerRecord() throws Exception {
    Method getter = LegacyContainerRecord.class.getMethod("getInner");
    Field fieldAnno = getter.getAnnotation(Field.class);
    Object result = manager.readDataAccordingFieldAnnotation(
        LegacyContainerRecord.class, "hello", getter, getter, fieldAnno);
    assertNotNull(result, "Nested @Record field result should not be null");
    assertInstanceOf(LegacyInnerRecord.class, result,
        "Result should be an instance of the nested record type");
    LegacyInnerRecord inner = (LegacyInnerRecord) result;
    assertEquals("hello", inner.getText(),
        "Recursively-loaded inner record should contain the correct value");
  }

  // --- F4: RuntimeException → ParseException wrap ---

  @Test
  public void readData_parseFails_throwsParseExceptionWithCause() throws Exception {
    Method getter = LegacyIntRecord.class.getMethod("getNumber");
    Field fieldAnno = getter.getAnnotation(Field.class);
    ParseException ex = assertThrows(ParseException.class, () ->
        manager.readDataAccordingFieldAnnotation(
            LegacyIntRecord.class, "XXXXX", getter, getter, fieldAnno)
    );
    assertNotNull(ex.getCause(),
        "ParseException should wrap the original RuntimeException as its cause");
    assertEquals("XXXXX", ex.getCompleteText(),
        "ParseException.completeText should be the full input data string");
    assertEquals(LegacyIntRecord.class, ex.getAnnotatedClass(),
        "ParseException.annotatedClass should be the record class passed to the method");
    assertEquals("getNumber", ex.getAnnotatedMethod().getName(),
        "ParseException.annotatedMethod should be the getter passed to the method");
  }

  @Test
  public void readData_parseFails_parseExceptionMessageContainsFieldInfo() throws Exception {
    Method getter = LegacyIntRecord.class.getMethod("getNumber");
    Field fieldAnno = getter.getAnnotation(Field.class);
    ParseException ex = assertThrows(ParseException.class, () ->
        manager.readDataAccordingFieldAnnotation(
            LegacyIntRecord.class, "XXXXX", getter, getter, fieldAnno)
    );
    String msg = ex.getMessage();
    assertNotNull(msg);
    assertTrue(msg.contains("XXXXX") || msg.contains("getNumber"),
        "ParseException message should reference the field or data: " + msg);
  }

  // --- F5: annotationSource differs from getter (field-level annotation) ---

  @Test
  public void readData_annotationSourceIsGetter_parsesCorrectly() throws Exception {
    Method getter = LegacySimpleRecord.class.getMethod("getText");
    Field fieldAnno = getter.getAnnotation(Field.class);
    // annotationSource same as getter — standard case
    Object result = manager.readDataAccordingFieldAnnotation(
        LegacySimpleRecord.class, "world     ", getter, (AnnotatedElement) getter, fieldAnno);
    assertEquals("world", result);
  }

  // --- Record fixtures for legacy API tests ---

  @Record
  public static class LegacySimpleRecord {
    private String text;

    @Field(offset = 1, length = 10)
    public String getText() { return text; }
    public void setText(String t) { this.text = t; }
  }

  @Record
  public static class LegacyIntRecord {
    private Integer number;

    @Field(offset = 1, length = 5, align = com.ancientprogramming.fixedformat4j.annotation.Align.RIGHT, paddingChar = '0')
    public Integer getNumber() { return number; }
    public void setNumber(Integer n) { this.number = n; }
  }

  @Record
  public static class LegacyRepeatingRecord {
    private String[] codes;

    @Field(offset = 1, length = 3, count = 3)
    public String[] getCodes() { return codes; }
    public void setCodes(String[] c) { this.codes = c; }
  }

  @Record
  public static class LegacyInnerRecord {
    private String text;

    @Field(offset = 1, length = 5)
    public String getText() { return text; }
    public void setText(String t) { this.text = t; }
  }

  @Record
  public static class LegacyContainerRecord {
    private LegacyInnerRecord inner;

    @Field(offset = 1, length = 5)
    public LegacyInnerRecord getInner() { return inner; }
    public void setInner(LegacyInnerRecord i) { this.inner = i; }
  }
}
