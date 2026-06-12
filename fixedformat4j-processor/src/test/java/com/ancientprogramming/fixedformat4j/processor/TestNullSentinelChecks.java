package com.ancientprogramming.fixedformat4j.processor;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ancientprogramming.fixedformat4j.processor.CompilationTestSupport.errorMessages;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mirrors FieldValidator.doValidateFieldNullChar and doValidateNullValue: the nullChar /
 * nullValue sentinels cannot represent null on primitive types, are mutually exclusive, and a
 * nullValue literal must exactly fill the field.
 */
class TestNullSentinelChecks {

  private static final String IMPORTS = "import com.ancientprogramming.fixedformat4j.annotation.*;\n"
          + "import com.ancientprogramming.fixedformat4j.annotation.Record;\n"
      + "import java.util.List;\n";

  @Test
  void nullCharOnPrimitiveFieldIsCompileError() {
    List<String> errors = errorMessages("NullCharOnPrimitive", IMPORTS
        + "@Record\n"
        + "public class NullCharOnPrimitive {\n"
        + "  @Field(offset = 1, length = 5, nullChar = ' ')\n"
        + "  public int getAmount() { return 0; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("nullChar"), errors.get(0));
    assertTrue(errors.get(0).contains("int"), errors.get(0));
  }

  @Test
  void nullCharOnBoxedFieldCompilesClean() {
    List<String> errors = errorMessages("NullCharOnBoxed", IMPORTS
        + "@Record\n"
        + "public class NullCharOnBoxed {\n"
        + "  @Field(offset = 1, length = 5, nullChar = ' ')\n"
        + "  public Integer getAmount() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void nullCharOnPrimitiveArrayElementTypeIsCompileError() {
    List<String> errors = errorMessages("NullCharOnPrimitiveArray", IMPORTS
        + "@Record\n"
        + "public class NullCharOnPrimitiveArray {\n"
        + "  @Field(offset = 1, length = 5, count = 3, nullChar = ' ')\n"
        + "  public int[] getAmounts() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("nullChar"), errors.get(0));
  }

  @Test
  void nullCharOnBoxedListElementTypeCompilesClean() {
    List<String> errors = errorMessages("NullCharOnBoxedList", IMPORTS
        + "@Record\n"
        + "public class NullCharOnBoxedList {\n"
        + "  @Field(offset = 1, length = 5, count = 3, nullChar = ' ')\n"
        + "  public List<Integer> getAmounts() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }

  @Test
  void nullValueAndNullCharTogetherIsCompileError() {
    List<String> errors = errorMessages("BothSentinels", IMPORTS
        + "@Record\n"
        + "public class BothSentinels {\n"
        + "  @Field(offset = 1, length = 4, nullChar = ' ', nullValue = \"9998\")\n"
        + "  public Integer getAmount() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("mutually exclusive"), errors.get(0));
  }

  @Test
  void nullValueLengthMismatchIsCompileError() {
    List<String> errors = errorMessages("NullValueWrongLength", IMPORTS
        + "@Record\n"
        + "public class NullValueWrongLength {\n"
        + "  @Field(offset = 1, length = 5, nullValue = \"9998\")\n"
        + "  public Integer getAmount() { return null; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("length"), errors.get(0));
  }

  @Test
  void nullValueOnPrimitiveFieldIsCompileError() {
    List<String> errors = errorMessages("NullValueOnPrimitive", IMPORTS
        + "@Record\n"
        + "public class NullValueOnPrimitive {\n"
        + "  @Field(offset = 1, length = 4, nullValue = \"9998\")\n"
        + "  public int getAmount() { return 0; }\n"
        + "}\n");
    assertEquals(1, errors.size(), "expected exactly one error: " + errors);
    assertTrue(errors.get(0).contains("nullValue"), errors.get(0));
  }

  @Test
  void matchingNullValueOnBoxedFieldCompilesClean() {
    List<String> errors = errorMessages("NullValueFits", IMPORTS
        + "@Record\n"
        + "public class NullValueFits {\n"
        + "  @Field(offset = 1, length = 4, nullValue = \"9998\")\n"
        + "  public Integer getAmount() { return null; }\n"
        + "}\n");
    assertEquals(List.of(), errors);
  }
}
