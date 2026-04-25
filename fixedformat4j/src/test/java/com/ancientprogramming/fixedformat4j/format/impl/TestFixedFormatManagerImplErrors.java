package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatEnum;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for error/exception paths in FixedFormatManagerImpl.
 */
public class TestFixedFormatManagerImplErrors {

  private FixedFormatManager manager;

  @BeforeEach
  public void setUp() {
    manager = new FixedFormatManagerImpl();
  }

  // --- load() error paths ---

  @Test
  public void load_classWithoutRecordAnnotation_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class, () -> manager.load(String.class, "some data"));
  }

  @Test
  public void load_classWithoutRecordAnnotation_exceptionMessageContainsClassAndRecordAnnotation() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(String.class, "some data"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("String") || msg.contains("java.lang.String"),
        "Message should contain class name: " + msg);
    assertTrue(msg.contains("record annotation"),
        "Message should mention 'record annotation': " + msg);
  }

  @Test
  public void load_classWithNoDefaultConstructor_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class, () ->
      manager.load(NoDefaultConstructorClass.MyInnerClass.class, "xyz       ")
    );
  }

  @Test
  public void load_unparsableData_throwsParseException() {
    ParseException ex = assertThrows(ParseException.class, () ->
      manager.load(SimpleIntRecord.class, "foobar")
    );
    assertEquals(SimpleIntRecord.class, ex.getAnnotatedClass());
    assertEquals("getNumber", ex.getAnnotatedMethod().getName());
    assertEquals("foobar", ex.getCompleteText());
  }

  @Test
  public void load_unparsableData_parseExceptionContainsFailedText() {
    ParseException ex = assertThrows(ParseException.class, () ->
      manager.load(SimpleIntRecord.class, "foobar")
    );
    assertNotNull(ex.getFailedText());
    assertNotNull(ex.getFormatContext());
    assertNotNull(ex.getFormatInstructions());
  }

  @Test
  public void load_unparsableData_parseExceptionHasCause() {
    ParseException ex = assertThrows(ParseException.class, () ->
        manager.load(SimpleIntRecord.class, "foobar")
    );
    assertNotNull(ex.getCause(), "ParseException should wrap the original cause");
  }

  // --- export() error paths ---

  @Test
  public void export_classWithoutRecordAnnotation_throwsFixedFormatException() {
    assertThrows(FixedFormatException.class, () -> manager.export("not annotated"));
  }

  @Test
  public void export_classWithoutRecordAnnotation_exceptionMessageContainsClassAndRecordAnnotation() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.export("not annotated"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("String") || msg.contains("java.lang.String"),
        "Message should contain class name: " + msg);
    assertTrue(msg.contains("record annotation"),
        "Message should mention 'record annotation': " + msg);
  }

  // --- Cluster A: doValidateEnumFieldLength ---

  @Test
  public void enumField_literalTooLong_throwsWithEnumClassAndLengths() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(EnumTooShortLiteralRecord.class, "RED"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("RgbColor"), "Message should contain enum class name: " + msg);
    assertTrue(msg.contains("5"), "Message should contain max length 5: " + msg);
    assertTrue(msg.contains("3"), "Message should contain field length 3: " + msg);
    assertTrue(msg.contains("getColor"), "Message should contain getter name: " + msg);
  }

  @Test
  public void enumField_numericTooLong_throwsWithMaxLengthAndFieldLength() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(EnumTooShortNumericRecord.class, "0"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("2"), "Message should contain max length 2 (ordinal 10 needs 2 digits): " + msg);
    assertTrue(msg.contains("1"), "Message should contain field length 1: " + msg);
    assertTrue(msg.contains("getVal"), "Message should contain getter name: " + msg);
  }

  @Test
  public void enumField_literalFitsExactly_doesNotThrow() {
    assertDoesNotThrow(() -> manager.load(EnumExactFitRecord.class, "RED  "));
  }

  // --- Cluster A: doValidateRestOfLineField ---

  @Test
  public void restOfLineField_nonStringType_throwsWithGetterAndTypeName() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(RestOfLineIntRecord.class, "42"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("getValue"), "Message should contain getter name: " + msg);
    assertTrue(msg.contains("int"), "Message should contain type 'int': " + msg);
    assertTrue(msg.contains("String"), "Message should mention String requirement: " + msg);
  }

  @Test
  public void restOfLineField_alignNotInherit_throwsWithGetterRef() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(RestOfLineAlignRecord.class, "hello"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("getValue"), "Message should contain getter name: " + msg);
    assertTrue(msg.contains("align"), "Message should mention 'align': " + msg);
  }

  @Test
  public void restOfLineField_nonDefaultPaddingChar_throwsWithGetterRef() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(RestOfLinePaddingRecord.class, "hello"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("getValue"), "Message should contain getter name: " + msg);
    assertTrue(msg.contains("paddingChar"), "Message should mention 'paddingChar': " + msg);
  }

  @Test
  public void restOfLineField_nullCharSet_throwsWithGetterRef() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(RestOfLineNullCharRecord.class, "hello"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("getValue"), "Message should contain getter name: " + msg);
    assertTrue(msg.contains("nullChar"), "Message should mention 'nullChar': " + msg);
  }

  // --- Cluster A: doValidateRestOfLineIsLastField ---

  @Test
  public void twoRestOfLineFields_throwsWithClassName() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(TwoRestOfLineRecord.class, "hello"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("TwoRestOfLineRecord"),
        "Message should contain record class name: " + msg);
    assertTrue(msg.contains("Only one") || msg.contains("multiple"),
        "Message should mention single-field constraint: " + msg);
  }

  @Test
  public void restOfLineFieldNotLast_throwsWithGetterRefAndOffset() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(RestOfLineNotLastRecord.class, "hello world"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("getNotes"), "Message should contain REST_OF_LINE getter name: " + msg);
    assertTrue(msg.contains("7"), "Message should contain maxOtherOffset=7: " + msg);
  }

  @Test
  public void repeatingFieldAfterRestOfLine_throwsWithRepeatingEndOffset() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(RepeatingAfterRestOfLineRecord.class, "hello"));
    String msg = ex.getMessage();
    // repeating field: offset=1, count=3, length=3 → effectiveEndOffset = 1 + 3*3 - 1 = 9
    // REST_OF_LINE at offset=5
    assertTrue(msg.contains("9"),
        "Message should contain effectiveEndOffset=9 (tests repeating formula): " + msg);
    assertTrue(msg.contains("getRest"), "Message should contain REST_OF_LINE getter name: " + msg);
  }

  // --- Cluster A: doValidateRestOfLineRecordLength ---

  @Test
  public void restOfLineWithFixedRecordLength_throwsWithRecordLengthInMessage() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(FixedLengthRestOfLineRecord.class, "hello"));
    String msg = ex.getMessage();
    assertTrue(msg.contains("20"), "Message should contain record length 20: " + msg);
    assertTrue(msg.contains("FixedLengthRestOfLineRecord"),
        "Message should contain class name: " + msg);
  }

  @Test
  public void restOfLineWithUnboundedRecordLength_doesNotThrow() {
    assertDoesNotThrow(() -> manager.load(ValidRestOfLineRecord.class, "hello world"));
  }

  // --- Cluster A: doValidateFieldNullChar (count > 1 path) ---

  @Test
  public void nullCharOnRepeatingPrimitiveArray_throwsWithPrimitiveTypeName() {
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(NullCharRepeatingIntRecord.class, "          "));
    String msg = ex.getMessage();
    assertTrue(msg.contains("int"), "Message should contain primitive type 'int': " + msg);
    assertTrue(msg.contains("getValues"), "Message should contain getter name: " + msg);
  }

  // --- Helper record classes ---

  @Record
  public static class SimpleIntRecord {
    private int number;

    @Field(offset = 1, length = 6, align = Align.RIGHT, paddingChar = '0')
    public int getNumber() {
      return number;
    }

    public void setNumber(int number) {
      this.number = number;
    }
  }

  // Enum validation fixtures

  enum RgbColor { RED, GREEN, BLUE }

  @Record
  public static class EnumTooShortLiteralRecord {
    private RgbColor color;

    @Field(offset = 1, length = 3)
    public RgbColor getColor() { return color; }
    public void setColor(RgbColor color) { this.color = color; }
  }

  enum ElevenValues { V0, V1, V2, V3, V4, V5, V6, V7, V8, V9, VA }

  @Record
  public static class EnumTooShortNumericRecord {
    private ElevenValues val;

    @Field(offset = 1, length = 1)
    @FixedFormatEnum(EnumFormat.NUMERIC)
    public ElevenValues getVal() { return val; }
    public void setVal(ElevenValues val) { this.val = val; }
  }

  enum ThreeColor { RED, GREEN, BLUE }

  @Record
  public static class EnumExactFitRecord {
    private ThreeColor color;

    @Field(offset = 1, length = 5)
    public ThreeColor getColor() { return color; }
    public void setColor(ThreeColor color) { this.color = color; }
  }

  // REST_OF_LINE validation fixtures

  @Record
  public static class RestOfLineIntRecord {
    private int value;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
  }

  @Record
  public static class RestOfLineAlignRecord {
    private String value;

    @Field(offset = 1, length = Field.REST_OF_LINE, align = Align.LEFT)
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
  }

  @Record
  public static class RestOfLinePaddingRecord {
    private String value;

    @Field(offset = 1, length = Field.REST_OF_LINE, paddingChar = 'X')
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
  }

  @Record
  public static class RestOfLineNullCharRecord {
    private String value;

    @Field(offset = 1, length = Field.REST_OF_LINE, nullChar = ' ')
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
  }

  // REST_OF_LINE is-last-field fixtures

  @Record
  public static class TwoRestOfLineRecord {
    private String first;
    private String second;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public String getFirst() { return first; }
    public void setFirst(String first) { this.first = first; }

    @Field(offset = 10, length = Field.REST_OF_LINE)
    public String getSecond() { return second; }
    public void setSecond(String second) { this.second = second; }
  }

  @Record
  public static class RestOfLineNotLastRecord {
    private String notes;
    private String suffix;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Field(offset = 5, length = 3)
    public String getSuffix() { return suffix; }
    public void setSuffix(String suffix) { this.suffix = suffix; }
  }

  @Record
  public static class RepeatingAfterRestOfLineRecord {
    private String[] codes;
    private String rest;

    @Field(offset = 1, length = 3, count = 3)
    public String[] getCodes() { return codes; }
    public void setCodes(String[] codes) { this.codes = codes; }

    @Field(offset = 5, length = Field.REST_OF_LINE)
    public String getRest() { return rest; }
    public void setRest(String rest) { this.rest = rest; }
  }

  // REST_OF_LINE record length fixtures

  @Record(length = 20)
  public static class FixedLengthRestOfLineRecord {
    private String value;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
  }

  @Record
  public static class ValidRestOfLineRecord {
    private String value;

    @Field(offset = 1, length = Field.REST_OF_LINE)
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
  }

  // NullChar on repeating primitive array fixture

  @Record
  public static class NullCharRepeatingIntRecord {
    private int[] values;

    @Field(offset = 1, length = 5, count = 2, nullChar = ' ')
    public int[] getValues() { return values; }
    public void setValues(int[] values) { this.values = values; }
  }
}
