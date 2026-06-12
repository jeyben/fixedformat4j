package com.ancientprogramming.fixedformat4j.processor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ancientprogramming.fixedformat4j.processor.CompilationTestSupport.errorMessages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Static layout checks WITHOUT a runtime counterpart (the issue #118 headline cases): a field
 * that runs past the declared @Record(length), and fields whose character ranges overlap.
 * These are stricter than the 1.8.x runtime, which silently tolerates both.
 */
class TestRecordLayoutChecks {

  private static final String IMPORTS = "import com.ancientprogramming.fixedformat4j.annotation.*;\n"
          + "import com.ancientprogramming.fixedformat4j.annotation.Record;\n";

  @Test
  void fieldExceedingRecordLengthIsCompileError() {
    List<String> errors = errorMessages("Overflowing", IMPORTS
        + "import java.util.Date;\n"
        + "@Record(length = 20)\n"
        + "public class Overflowing {\n"
        + "  @Field(offset = 1, length = 36)\n"
        + "  public Date getCreatedAt() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("@Field length 36 exceeds @Record length 20"), errors.get(0));
    assertTrue(errors.get(0).contains("getCreatedAt"), errors.get(0));
  }

  @Test
  void repeatingFieldExceedingRecordLengthIsCompileError() {
    List<String> errors = errorMessages("RepeatOverflow", IMPORTS
        + "import java.util.List;\n"
        + "@Record(length = 20)\n"
        + "public class RepeatOverflow {\n"
        + "  @Field(offset = 1, length = 8, count = 3)\n"
        + "  public List<String> getCodes() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("exceeds @Record length 20"), errors.get(0));
  }

  @Test
  void fieldEndingExactlyAtRecordLengthCompilesClean() {
    List<String> errors = errorMessages("ExactFit", IMPORTS
        + "@Record(length = 20)\n"
        + "public class ExactFit {\n"
        + "  @Field(offset = 1, length = 10)\n"
        + "  public String getHead() { return null; }\n"
        + "  @Field(offset = 11, length = 10)\n"
        + "  public String getTail() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void noRecordLengthMeansNoOverflowCheck() {
    List<String> errors = errorMessages("Unbounded", IMPORTS
        + "@Record\n"
        + "public class Unbounded {\n"
        + "  @Field(offset = 1, length = 500)\n"
        + "  public String getBlob() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void duplicateOffsetsAreCompileError() {
    List<String> errors = errorMessages("DuplicateOffsets", IMPORTS
        + "@Record\n"
        + "public class DuplicateOffsets {\n"
        + "  @Field(offset = 1, length = 10)\n"
        + "  public String getFirst() { return null; }\n"
        + "  @Field(offset = 1, length = 10)\n"
        + "  public String getSecond() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("overlaps"), errors.get(0));
  }

  @Test
  void partiallyOverlappingFieldsAreCompileError() {
    List<String> errors = errorMessages("Overlapping", IMPORTS
        + "@Record\n"
        + "public class Overlapping {\n"
        + "  @Field(offset = 1, length = 10)\n"
        + "  public String getFirst() { return null; }\n"
        + "  @Field(offset = 5, length = 10)\n"
        + "  public String getSecond() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("getFirst"), errors.get(0));
    assertTrue(errors.get(0).contains("getSecond"), errors.get(0));
  }

  @Test
  void wideFieldOverlappingSeveralNarrowerFieldsReportsEveryOverlapInOnePass() {
    List<String> errors = errorMessages("WideOverlap", IMPORTS
        + "@Record\n"
        + "public class WideOverlap {\n"
        + "  @Field(offset = 1, length = 20)\n"
        + "  public String getWide() { return null; }\n"
        + "  @Field(offset = 5, length = 3)\n"
        + "  public String getInnerOne() { return null; }\n"
        + "  @Field(offset = 10, length = 3)\n"
        + "  public String getInnerTwo() { return null; }\n"
        + "}\n");
    assertEquals(2, errors.size(), "both overlaps with the wide field must be reported: " + errors);
    assertTrue(errors.get(0).contains("getWide") && errors.get(0).contains("getInnerOne"), errors.get(0));
    assertTrue(errors.get(1).contains("getWide") && errors.get(1).contains("getInnerTwo"), errors.get(1));
  }

  @Test
  void repeatingFieldOverlapIsDetectedAcrossExpandedRange() {
    List<String> errors = errorMessages("RepeatOverlap", IMPORTS
        + "import java.util.List;\n"
        + "@Record\n"
        + "public class RepeatOverlap {\n"
        + "  @Field(offset = 1, length = 5, count = 4)\n"
        + "  public List<String> getCodes() { return null; }\n"
        + "  @Field(offset = 18, length = 5)\n"
        + "  public String getTail() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
  }

  @Test
  void adjacentFieldsCompileClean() {
    List<String> errors = errorMessages("Adjacent", IMPORTS
        + "@Record\n"
        + "public class Adjacent {\n"
        + "  @Field(offset = 1, length = 10)\n"
        + "  public String getFirst() { return null; }\n"
        + "  @Field(offset = 11, length = 10)\n"
        + "  public String getSecond() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void multiFormatFieldsAtDistinctOffsetsCompileClean() {
    List<String> errors = errorMessages("MultiFormat", IMPORTS
        + "@Record\n"
        + "public class MultiFormat {\n"
        + "  @Fields({\n"
        + "    @Field(offset = 1, length = 10),\n"
        + "    @Field(offset = 11, length = 10, align = Align.RIGHT, paddingChar = '0')\n"
        + "  })\n"
        + "  public String getValue() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }
}
