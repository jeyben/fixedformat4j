package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.RecordAlign;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatBoolean;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatDecimal;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatNumber;
import com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.annotation.Sign;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FormatContext;
import com.ancientprogramming.fixedformat4j.format.FormatInstructions;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatBooleanData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatDecimalData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatNumberData;
import com.ancientprogramming.fixedformat4j.format.data.FixedFormatPatternData;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class TestFormatInstructionsBuilder {

  private final FormatInstructionsBuilder builder = new FormatInstructionsBuilder();

  @Test
  void build_defaultAnnotations_returnsDefaultData() throws Exception {
    Method getter = SimpleRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    FormatInstructions instructions = builder.build(getter, fieldAnno, getter.getReturnType(), SimpleRecord.class);
    assertSame(FixedFormatPatternData.DEFAULT, instructions.getFixedFormatPatternData());
    assertSame(FixedFormatBooleanData.DEFAULT, instructions.getFixedFormatBooleanData());
    assertSame(FixedFormatNumberData.DEFAULT, instructions.getFixedFormatNumberData());
    assertSame(FixedFormatDecimalData.DEFAULT, instructions.getFixedFormatDecimalData());
  }

  @Test
  void build_withFixedFormatPattern_capturesPattern() throws Exception {
    Method getter = PatternRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    FormatInstructions instructions = builder.build(getter, fieldAnno, getter.getReturnType(), PatternRecord.class);
    assertEquals("yyyyMMdd", instructions.getFixedFormatPatternData().getPattern());
  }

  @Test
  void build_withFixedFormatBoolean_capturesTrueFalseValues() throws Exception {
    Method getter = BoolRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    FormatInstructions instructions = builder.build(getter, fieldAnno, getter.getReturnType(), BoolRecord.class);
    assertEquals("Y", instructions.getFixedFormatBooleanData().getTrueValue());
    assertEquals("N", instructions.getFixedFormatBooleanData().getFalseValue());
  }

  @Test
  void build_withFixedFormatNumber_capturesSignConfig() throws Exception {
    Method getter = NumberRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    FormatInstructions instructions = builder.build(getter, fieldAnno, getter.getReturnType(), NumberRecord.class);
    assertEquals(Sign.PREPEND, instructions.getFixedFormatNumberData().getSigning());
  }

  @Test
  void build_withFixedFormatDecimal_capturesDecimals() throws Exception {
    Method getter = DecimalRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    FormatInstructions instructions = builder.build(getter, fieldAnno, getter.getReturnType(), DecimalRecord.class);
    assertEquals(2, instructions.getFixedFormatDecimalData().getDecimals());
  }

  @Test
  void build_inheritAlign_resolvesFromRecordAnnotation() throws Exception {
    Method getter = RightAlignRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    FormatInstructions instructions = builder.build(getter, fieldAnno, getter.getReturnType(), RightAlignRecord.class);
    assertEquals(Align.RIGHT, instructions.getAlignment());
  }

  @Test
  void build_explicitFieldAlign_overridesRecordDefault() throws Exception {
    Method getter = OverrideAlignRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    FormatInstructions instructions = builder.build(getter, fieldAnno, getter.getReturnType(), OverrideAlignRecord.class);
    assertEquals(Align.LEFT, instructions.getAlignment());
  }

  @Test
  void build_inheritAlignWithNoRecordAnnotation_defaultsToLeft() throws Exception {
    Method getter = SimpleRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    FormatInstructions instructions = builder.build(getter, fieldAnno, getter.getReturnType(), SimpleRecord.class);
    assertEquals(Align.LEFT, instructions.getAlignment());
  }

  @Test
  void context_fromFieldAnnotation_hasCorrectOffsetAndType() throws Exception {
    Method getter = SimpleRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    FormatContext<?> context = builder.context(String.class, fieldAnno);
    assertEquals(5, context.getOffset());
    assertEquals(String.class, context.getDataType());
  }

  @Test
  void datatype_beanStandardMethod_returnsReturnType() throws Exception {
    Method getter = SimpleRecord.class.getMethod("getValue");
    Field fieldAnno = getter.getAnnotation(Field.class);
    assertEquals(String.class, builder.datatype(getter, fieldAnno));
  }

  @Test
  void datatype_nonBeanStandardMethod_throwsFixedFormatException() throws Exception {
    Method nonGetter = NonGetterRecord.class.getMethod("fetch");
    Field fieldAnno = nonGetter.getAnnotation(Field.class);
    assertThrows(FixedFormatException.class, () -> builder.datatype(nonGetter, fieldAnno));
  }

  // --- Fixture classes ---

  @Record
  static class SimpleRecord {
    @Field(offset = 5, length = 3)
    public String getValue() { return null; }
    public void setValue(String v) {}
  }

  @Record
  static class PatternRecord {
    @Field(offset = 1, length = 8)
    @FixedFormatPattern("yyyyMMdd")
    public String getValue() { return null; }
    public void setValue(String v) {}
  }

  @Record
  static class BoolRecord {
    @Field(offset = 1, length = 1)
    @FixedFormatBoolean(trueValue = "Y", falseValue = "N")
    public Boolean getValue() { return null; }
    public void setValue(Boolean v) {}
  }

  @Record
  static class NumberRecord {
    @Field(offset = 1, length = 10)
    @FixedFormatNumber(sign = Sign.PREPEND)
    public Integer getValue() { return null; }
    public void setValue(Integer v) {}
  }

  @Record
  static class DecimalRecord {
    @Field(offset = 1, length = 10)
    @FixedFormatDecimal(decimals = 2)
    public BigDecimal getValue() { return null; }
    public void setValue(BigDecimal v) {}
  }

  @Record
  static class NonGetterRecord {
    @Field(offset = 1, length = 5)
    public String fetch() { return null; }
  }

  @Record(align = RecordAlign.RIGHT)
  static class RightAlignRecord {
    @Field(offset = 1, length = 5)
    public String getValue() { return null; }
    public void setValue(String v) {}
  }

  @Record(align = RecordAlign.RIGHT)
  static class OverrideAlignRecord {
    @Field(offset = 1, length = 5, align = Align.LEFT)
    public String getValue() { return null; }
    public void setValue(String v) {}
  }
}
