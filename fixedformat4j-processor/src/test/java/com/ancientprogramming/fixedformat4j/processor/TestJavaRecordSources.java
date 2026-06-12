package com.ancientprogramming.fixedformat4j.processor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import java.util.List;

import static com.ancientprogramming.fixedformat4j.processor.CompilationTestSupport.errorMessages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The same checks must fire for Java record types, where @Field sits on record components and
 * propagates to both the backing field and the accessor. The scanner must count each component
 * once — a naive scan of both members would produce false duplicate-offset errors.
 *
 * <p>Record sources need JDK 16+ syntax, so these tests only run on newer JREs (matching the
 * jdk17-tests convention of the core module); the processor itself stays at release 11.
 */
@EnabledForJreRange(min = JRE.JAVA_17)
class TestJavaRecordSources {

  private static final String IMPORTS = "import com.ancientprogramming.fixedformat4j.annotation.*;\n"
          + "import com.ancientprogramming.fixedformat4j.annotation.Record;\n";

  @Test
  void validJavaRecordCompilesClean() {
    List<String> errors = errorMessages("ValidJavaRecord", IMPORTS
        + "@Record(length = 20)\n"
        + "public record ValidJavaRecord(\n"
        + "    @Field(offset = 1, length = 10) String name,\n"
        + "    @Field(offset = 11, length = 10) Integer amount) {\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void invalidPatternOnRecordComponentIsCompileError() {
    List<String> errors = errorMessages("BadPatternRecord", IMPORTS
        + "import java.util.Date;\n"
        + "@Record\n"
        + "public record BadPatternRecord(\n"
        + "    @Field(offset = 1, length = 18) @FixedFormatPattern(\"not-a-date-pattern\") Date createdAt) {\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("not-a-date-pattern"), errors.get(0));
  }

  @Test
  void fieldExceedingRecordLengthOnRecordComponentIsCompileError() {
    List<String> errors = errorMessages("OverflowRecord", IMPORTS
        + "import java.util.Date;\n"
        + "@Record(length = 20)\n"
        + "public record OverflowRecord(\n"
        + "    @Field(offset = 1, length = 36) Date createdAt) {\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("@Field length 36 exceeds @Record length 20"), errors.get(0));
  }

  @Test
  void nullCharOnPrimitiveRecordComponentIsCompileError() {
    List<String> errors = errorMessages("PrimitiveNullCharRecord", IMPORTS
        + "@Record\n"
        + "public record PrimitiveNullCharRecord(\n"
        + "    @Field(offset = 1, length = 5, nullChar = ' ') int amount) {\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("nullChar"), errors.get(0));
  }

  @Test
  void componentAccessorWithoutGetPrefixIsScanned() {
    List<String> errors = errorMessages("IssuerRecord", IMPORTS
        + "@Record\n"
        + "public record IssuerRecord(\n"
        + "    @Field(offset = 1, length = 10) String issuer,\n"
        + "    @Field(offset = 5, length = 10) String reference) {\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one overlap error: " + errors);
    assertTrue(errors.get(0).contains("overlaps"), errors.get(0));
  }
}
