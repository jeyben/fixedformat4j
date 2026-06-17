package com.ancientprogramming.fixedformat4j.processor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ancientprogramming.fixedformat4j.processor.CompilationTestSupport.errorMessages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mirrors FieldValidator.doValidateEnumFieldLength: an enum field whose widest serialized form
 * cannot fit in @Field(length) is a compile error. LITERAL uses the longest constant name,
 * NUMERIC the digit width of the highest ordinal.
 */
class TestEnumLengthCheck {

  private static final String IMPORTS = "import com.ancientprogramming.fixedformat4j.annotation.*;\n"
          + "import com.ancientprogramming.fixedformat4j.annotation.Record;\n";

  private static final String STATUS_ENUM =
      "enum Status { OK, REJECTED_BY_BANK }\n";

  @Test
  void literalEnumWiderThanFieldLengthIsCompileError() {
    List<String> errors = errorMessages("LiteralTooNarrow", IMPORTS
        + STATUS_ENUM
        + "@Record\n"
        + "public class LiteralTooNarrow {\n"
        + "  @Field(offset = 1, length = 5)\n"
        + "  public Status getStatus() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("16"), errors.get(0));
    assertTrue(errors.get(0).contains("getStatus"), errors.get(0));
  }

  @Test
  void literalEnumFittingFieldLengthCompilesClean() {
    List<String> errors = errorMessages("LiteralFits", IMPORTS
        + STATUS_ENUM
        + "@Record\n"
        + "public class LiteralFits {\n"
        + "  @Field(offset = 1, length = 16)\n"
        + "  public Status getStatus() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void numericEnumUsesOrdinalDigitWidth() {
    List<String> errors = errorMessages("NumericFits", IMPORTS
        + STATUS_ENUM
        + "@Record\n"
        + "public class NumericFits {\n"
        + "  @Field(offset = 1, length = 1)\n"
        + "  @FixedFormatEnum(EnumFormat.NUMERIC)\n"
        + "  public Status getStatus() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void numericEnumWithTooManyConstantsForFieldLengthIsCompileError() {
    StringBuilder constants = new StringBuilder();
    for (int i = 0; i < 11; i++) {
      constants.append("V").append(i).append(i == 10 ? "" : ", ");
    }
    List<String> errors = errorMessages("NumericTooNarrow", IMPORTS
        + "enum Wide { " + constants + " }\n"
        + "@Record\n"
        + "public class NumericTooNarrow {\n"
        + "  @Field(offset = 1, length = 1)\n"
        + "  @FixedFormatEnum(EnumFormat.NUMERIC)\n"
        + "  public Wide getValue() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
  }

  @Test
  void enumFieldWithCustomFormatterIsSkippedLikeAtRuntime() {
    // Status.REJECTED_BY_BANK is 16 chars, far wider than length = 1; the default-formatter
    // path would error, but a custom formatter emits its own representation so the check is
    // skipped — mirroring FieldValidator.doValidateEnumFieldLength (issue #161).
    List<String> errors = errorMessages("CustomFormatterEnum",
        "import com.ancientprogramming.fixedformat4j.annotation.*;\n"
        + "import com.ancientprogramming.fixedformat4j.annotation.Record;\n"
        + "import com.ancientprogramming.fixedformat4j.format.AbstractFixedFormatter;\n"
        + "import com.ancientprogramming.fixedformat4j.format.FormatInstructions;\n"
        + STATUS_ENUM
        + "class StatusFormatter extends AbstractFixedFormatter<Status> {\n"
        + "  public Status asObject(String value, FormatInstructions instructions) { return null; }\n"
        + "  public String asString(Status value, FormatInstructions instructions) { return \"\"; }\n"
        + "}\n"
        + "@Record\n"
        + "public class CustomFormatterEnum {\n"
        + "  @Field(offset = 1, length = 1, formatter = StatusFormatter.class)\n"
        + "  public Status getStatus() { return null; }\n"
        + "}\n");
    assertTrue(errors.stream().noneMatch(message -> message.contains("max length")),
        "enum length check must not fire when a custom formatter is declared: " + errors);
  }

  @Test
  void restOfLineEnumFieldIsSkippedLikeAtRuntime() {
    List<String> errors = errorMessages("RestOfLineEnumSkipped", IMPORTS
        + STATUS_ENUM
        + "@Record\n"
        + "public class RestOfLineEnumSkipped {\n"
        + "  @Field(offset = 1, length = -1)\n"
        + "  public Status getStatus() { return null; }\n"
        + "}\n");
    assertTrue(errors.stream().noneMatch(message -> message.contains("max length")),
        "enum length check must not fire for REST_OF_LINE fields: " + errors);
  }
}
