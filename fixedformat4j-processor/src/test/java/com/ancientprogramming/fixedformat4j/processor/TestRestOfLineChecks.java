package com.ancientprogramming.fixedformat4j.processor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ancientprogramming.fixedformat4j.processor.CompilationTestSupport.errorMessages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mirrors FieldValidator.doValidateRestOfLineField / doValidateRestOfLineIsLastField /
 * doValidateRestOfLineRecordLength: a @Field(length = -1) rest-of-line field is String-only,
 * non-repeating, takes no padding/alignment/null sentinels, must be the only and last one in
 * the record, and is incompatible with a fixed @Record(length).
 */
class TestRestOfLineChecks {

  private static final String IMPORTS = "import com.ancientprogramming.fixedformat4j.annotation.*;\n"
          + "import com.ancientprogramming.fixedformat4j.annotation.Record;\n";

  @Test
  void restOfLineOnNonStringFieldIsCompileError() {
    List<String> errors = errorMessages("RolNonString", IMPORTS
        + "@Record\n"
        + "public class RolNonString {\n"
        + "  @Field(offset = 1, length = -1)\n"
        + "  public Integer getRest() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("String"), errors.get(0));
  }

  @Test
  void restOfLineWithCountIsCompileError() {
    List<String> errors = errorMessages("RolWithCount", IMPORTS
        + "@Record\n"
        + "public class RolWithCount {\n"
        + "  @Field(offset = 1, length = -1, count = 2)\n"
        + "  public String getRest() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("count"), errors.get(0));
  }

  @Test
  void restOfLineWithExplicitAlignIsCompileError() {
    List<String> errors = errorMessages("RolWithAlign", IMPORTS
        + "@Record\n"
        + "public class RolWithAlign {\n"
        + "  @Field(offset = 1, length = -1, align = Align.RIGHT)\n"
        + "  public String getRest() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("align"), errors.get(0));
  }

  @Test
  void restOfLineWithExplicitPaddingCharIsCompileError() {
    List<String> errors = errorMessages("RolWithPadding", IMPORTS
        + "@Record\n"
        + "public class RolWithPadding {\n"
        + "  @Field(offset = 1, length = -1, paddingChar = '0')\n"
        + "  public String getRest() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("paddingChar"), errors.get(0));
  }

  @Test
  void restOfLineWithNullCharIsCompileError() {
    List<String> errors = errorMessages("RolWithNullChar", IMPORTS
        + "@Record\n"
        + "public class RolWithNullChar {\n"
        + "  @Field(offset = 1, length = -1, nullChar = ' ')\n"
        + "  public String getRest() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("nullChar"), errors.get(0));
  }

  @Test
  void restOfLineWithNullValueIsCompileError() {
    List<String> errors = errorMessages("RolWithNullValue", IMPORTS
        + "@Record\n"
        + "public class RolWithNullValue {\n"
        + "  @Field(offset = 1, length = -1, nullValue = \"NONE\")\n"
        + "  public String getRest() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("nullValue"), errors.get(0));
  }

  @Test
  void multipleRestOfLineFieldsIsCompileError() {
    List<String> errors = errorMessages("TwoRols", IMPORTS
        + "@Record\n"
        + "public class TwoRols {\n"
        + "  @Field(offset = 11, length = -1)\n"
        + "  public String getFirst() { return null; }\n"
        + "  @Field(offset = 21, length = -1)\n"
        + "  public String getSecond() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("Only one"), errors.get(0));
  }

  @Test
  void restOfLineNotLastByOffsetIsCompileError() {
    List<String> errors = errorMessages("RolNotLast", IMPORTS
        + "@Record\n"
        + "public class RolNotLast {\n"
        + "  @Field(offset = 5, length = -1)\n"
        + "  public String getRest() { return null; }\n"
        + "  @Field(offset = 1, length = 10)\n"
        + "  public String getHead() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("last"), errors.get(0));
  }

  @Test
  void restOfLineWithFixedRecordLengthIsCompileError() {
    List<String> errors = errorMessages("RolFixedRecord", IMPORTS
        + "@Record(length = 30)\n"
        + "public class RolFixedRecord {\n"
        + "  @Field(offset = 1, length = 10)\n"
        + "  public String getHead() { return null; }\n"
        + "  @Field(offset = 11, length = -1)\n"
        + "  public String getRest() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("@Record"), errors.get(0));
  }

  @Test
  void validTrailingRestOfLineCompilesClean() {
    List<String> errors = errorMessages("RolValid", IMPORTS
        + "@Record\n"
        + "public class RolValid {\n"
        + "  @Field(offset = 1, length = 10)\n"
        + "  public String getHead() { return null; }\n"
        + "  @Field(offset = 11, length = -1)\n"
        + "  public String getRest() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }
}
