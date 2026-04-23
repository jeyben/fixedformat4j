package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderBuilder {

  @Record(length = 10)
  static class SampleRecord {
    private String data;

    @Field(offset = 1, length = 10)
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
  }

  private final FixedFormatMatchPattern anyPattern = new RegexFixedFormatMatchPattern(".*");

  @Test
  void buildsSuccessfullyWithOneMapping() {
    FixedFormatReader<SampleRecord> reader = FixedFormatReader.<SampleRecord>builder()
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
  void throwsIllegalArgumentWhenAddMappingClassIsNull() {
    assertThrows(IllegalArgumentException.class, () ->
        FixedFormatReader.builder().addMapping(null, anyPattern)
    );
  }

  @Test
  void throwsIllegalArgumentWhenAddMappingPatternIsNull() {
    assertThrows(IllegalArgumentException.class, () ->
        FixedFormatReader.<SampleRecord>builder().addMapping(SampleRecord.class, null)
    );
  }

  @Test
  void defaultManagerParsesRecordsWithoutExplicitManagerCall() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, anyPattern)
        .manager(FixedFormatManagerImpl.create())
        .build();
    List<TenCharRecord> results = reader.readAsList(new StringReader("hello     "));
    assertEquals(1, results.size());
    assertEquals("hello", results.get(0).getValue());
  }

  @Test
  void builderIsFluentReturningItself() {
    FixedFormatReader.Builder<SampleRecord> builder = FixedFormatReader.builder();
    assertSame(builder, builder.addMapping(SampleRecord.class, anyPattern));
    assertSame(builder, builder.multiMatchStrategy(MultiMatchStrategy.firstMatch()));
    assertSame(builder, builder.unmatchedLineStrategy(UnmatchedLineStrategy.skip()));
    assertSame(builder, builder.parseErrorStrategy(ParseErrorStrategy.throwException()));
  }

  @Test
  void unmatchedLambdaStrategyIsAcceptedByBuilder() {
    FixedFormatReader<SampleRecord> reader = FixedFormatReader.<SampleRecord>builder()
        .addMapping(SampleRecord.class, anyPattern)
        .unmatchedLineStrategy((lineNumber, line) -> {})
        .build();
    assertNotNull(reader);
  }

  @Test
  void parseErrorLambdaStrategyIsAcceptedByBuilder() {
    FixedFormatReader<SampleRecord> reader = FixedFormatReader.<SampleRecord>builder()
        .addMapping(SampleRecord.class, anyPattern)
        .parseErrorStrategy((wrapped, line, lineNumber) -> {})
        .build();
    assertNotNull(reader);
  }
}
