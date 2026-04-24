package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import com.ancientprogramming.fixedformat4j.io.read.ParseErrorStrategy;
import com.ancientprogramming.fixedformat4j.io.read.UnmatchStrategy;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;
import com.ancientprogramming.fixedformat4j.io.read.MultiMatchStrategy;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReaderBuilder;

class TestFixedFormatReaderBuilder {

  @Record(length = 10)
  static class SampleRecord {
    private String data;

    @Field(offset = 1, length = 10)
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
  }

  private final Predicate<String> anyPattern = Pattern.compile(".*").asPredicate();

  @Test
  void buildsSuccessfullyWithOneMapping() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(SampleRecord.class, anyPattern)
        .build();
    assertNotNull(reader);
  }

  @Test
  void throwsWhenNoMappings() {
    assertThrows(IllegalArgumentException.class, () ->
        FixedFormatReader.builder().build()
    );
  }

  @Test
  void throwsNullPointerWhenAddMappingClassIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatReader.builder().addMapping(null, anyPattern)
    );
  }

  @Test
  void throwsNullPointerWhenAddMappingPatternIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatReader.builder().addMapping(SampleRecord.class, null)
    );
  }

  @Test
  void defaultManagerParsesRecordsWithoutExplicitManagerCall() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, anyPattern)
        .manager(FixedFormatManagerImpl.create())
        .build();
    List<TenCharRecord> results = reader
        .read(new StringReader("hello     "))
        .get(TenCharRecord.class);
    assertEquals(1, results.size());
    assertEquals("hello", results.get(0).getValue());
  }

  @Test
  void builderIsFluentReturningItself() {
    FixedFormatReaderBuilder builder = FixedFormatReader.builder();
    assertSame(builder, builder.addMapping(SampleRecord.class, anyPattern));
    assertSame(builder, builder.multiMatchStrategy(MultiMatchStrategy.firstMatch()));
    assertSame(builder, builder.unmatchStrategy(UnmatchStrategy.skip()));
    assertSame(builder, builder.parseErrorStrategy(ParseErrorStrategy.throwException()));
  }

  @Test
  void throwsNullPointerWhenMultiMatchStrategyIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatReader.builder()
            .addMapping(SampleRecord.class, anyPattern)
            .multiMatchStrategy(null)
    );
  }

  @Test
  void throwsNullPointerWhenUnmatchStrategyIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatReader.builder()
            .addMapping(SampleRecord.class, anyPattern)
            .unmatchStrategy(null)
    );
  }

  @Test
  void throwsNullPointerWhenParseErrorStrategyIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatReader.builder()
            .addMapping(SampleRecord.class, anyPattern)
            .parseErrorStrategy(null)
    );
  }

  @Test
  void throwsNullPointerWhenManagerIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatReader.builder()
            .addMapping(SampleRecord.class, anyPattern)
            .manager(null)
    );
  }

  @Test
  void throwsNullPointerWhenExcludeLinesPredicateIsNull() {
    assertThrows(NullPointerException.class, () ->
        FixedFormatReader.builder()
            .addMapping(SampleRecord.class, anyPattern)
            .excludeLines(null)
    );
  }

  @Test
  void unmatchedLambdaStrategyIsAcceptedByBuilder() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(SampleRecord.class, anyPattern)
        .unmatchStrategy((lineNumber, line) -> {})
        .build();
    assertNotNull(reader);
  }

  @Test
  void parseErrorLambdaStrategyIsAcceptedByBuilder() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(SampleRecord.class, anyPattern)
        .parseErrorStrategy((wrapped, line, lineNumber) -> {})
        .build();
    assertNotNull(reader);
  }
}
