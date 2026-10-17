package com.ancientprogramming.fixedformat4j.processor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ancientprogramming.fixedformat4j.processor.CompilationTestSupport.errorMessages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mirrors the runtime pattern validation (FieldValidator.doValidateFieldPattern +
 * PatternValidator): an invalid date/time pattern on a Date, LocalDate or LocalDateTime field
 * is a compile error; valid and absent patterns compile clean; non-date types ignore the
 * pattern annotation just like the runtime does.
 */
class TestPatternCheck {

  private static final String IMPORTS =
      "import com.ancientprogramming.fixedformat4j.annotation.*;\n"
          + "import com.ancientprogramming.fixedformat4j.annotation.Record;\n"
          + "import java.util.Date;\n"
          + "import java.time.LocalDate;\n"
          + "import java.time.LocalDateTime;\n";

  @Test
  void invalidPatternOnDateFieldIsCompileError() {
    List<String> errors = errorMessages("BadDatePattern", IMPORTS
        + "@Record\n"
        + "public class BadDatePattern {\n"
        + "  @Field(offset = 1, length = 18)\n"
        + "  @FixedFormatPattern(\"not-a-date-pattern\")\n"
        + "  public Date getCreatedAt() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("not-a-date-pattern"), errors.get(0));
    assertTrue(errors.get(0).contains("getCreatedAt"), errors.get(0));
  }

  @Test
  void invalidPatternOnLocalDateFieldIsCompileError() {
    List<String> errors = errorMessages("BadLocalDatePattern", IMPORTS
        + "@Record\n"
        + "public class BadLocalDatePattern {\n"
        + "  @Field(offset = 1, length = 8)\n"
        + "  @FixedFormatPattern(\"bbbb\")\n"
        + "  public LocalDate getDay() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("bbbb"), errors.get(0));
  }

  @Test
  void invalidPatternOnLocalDateTimeFieldIsCompileError() {
    List<String> errors = errorMessages("BadLocalDateTimePattern", IMPORTS
        + "@Record\n"
        + "public class BadLocalDateTimePattern {\n"
        + "  @Field(offset = 1, length = 14)\n"
        + "  @FixedFormatPattern(\"bbbb\")\n"
        + "  public LocalDateTime getTimestamp() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
  }

  @Test
  void validPatternCompilesClean() {
    List<String> errors = errorMessages("GoodPattern", IMPORTS
        + "@Record\n"
        + "public class GoodPattern {\n"
        + "  @Field(offset = 1, length = 8)\n"
        + "  @FixedFormatPattern(\"yyyyMMdd\")\n"
        + "  public Date getCreatedAt() { return null; }\n"
        + "  @Field(offset = 9, length = 8)\n"
        + "  @FixedFormatPattern(\"yyyyMMdd\")\n"
        + "  public LocalDate getDay() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void absentPatternAnnotationCompilesClean() {
    List<String> errors = errorMessages("DefaultPattern", IMPORTS
        + "@Record\n"
        + "public class DefaultPattern {\n"
        + "  @Field(offset = 1, length = 8)\n"
        + "  public Date getCreatedAt() { return null; }\n"
        + "  @Field(offset = 9, length = 8)\n"
        + "  public LocalDate getDay() { return null; }\n"
        + "  @Field(offset = 17, length = 14)\n"
        + "  public LocalDateTime getTimestamp() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void patternOnNonDateTypeIsIgnoredLikeAtRuntime() {
    List<String> errors = errorMessages("PatternOnString", IMPORTS
        + "@Record\n"
        + "public class PatternOnString {\n"
        + "  @Field(offset = 1, length = 10)\n"
        + "  @FixedFormatPattern(\"not-a-date-pattern\")\n"
        + "  public String getName() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void invalidPatternOnStaticGetterIsCompileErrorLikeAtRuntime() {
    List<String> errors = errorMessages("StaticGetter", IMPORTS
        + "@Record\n"
        + "public class StaticGetter {\n"
        + "  @Field(offset = 1, length = 18)\n"
        + "  @FixedFormatPattern(\"not-a-date-pattern\")\n"
        + "  public static Date getCreatedAt() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(),
        "the runtime AnnotationScanner picks up static getters (Class.getMethods includes them),"
            + " so the processor must validate them too: " + errors);
  }

  @Test
  void invalidPatternOnRepeatingLocalDateFieldIsCompileError() {
    // Issue 182 (bug #3): the check used to resolve the raw List return type instead of the
    // element type for count > 1 fields, so it never matched LocalDate and silently no-opped.
    List<String> errors = errorMessages("BadRepeatingLocalDatePattern", IMPORTS
        + "import java.util.List;\n"
        + "@Record\n"
        + "public class BadRepeatingLocalDatePattern {\n"
        + "  @Field(offset = 1, length = 8, count = 2)\n"
        + "  @FixedFormatPattern(\"bbbb\")\n"
        + "  public List<LocalDate> getDays() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("bbbb"), errors.get(0));
  }

  @Test
  void invalidPatternOnAnnotatedFieldMemberIsCompileError() {
    List<String> errors = errorMessages("BadFieldMemberPattern", IMPORTS
        + "@Record\n"
        + "public class BadFieldMemberPattern {\n"
        + "  @Field(offset = 1, length = 18)\n"
        + "  @FixedFormatPattern(\"not-a-date-pattern\")\n"
        + "  private Date createdAt;\n"
        + "  public Date getCreatedAt() { return createdAt; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("not-a-date-pattern"), errors.get(0));
  }
}
