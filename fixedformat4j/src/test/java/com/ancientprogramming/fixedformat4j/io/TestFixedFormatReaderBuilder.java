package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import org.junit.jupiter.api.Test;

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
  void throwsWhenUnmatchedHandlerStrategySetWithoutHandler() {
    IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        FixedFormatReader.<SampleRecord>builder()
            .addMapping(SampleRecord.class, anyPattern)
            .unmatchedLineStrategy(UnmatchedLineStrategy.FORWARD_TO_HANDLER)
            .build()
    );
    assertTrue(ex.getMessage().contains("unmatchedLineHandler"));
  }

  @Test
  void throwsWhenParseErrorHandlerStrategySetWithoutHandler() {
    IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
        FixedFormatReader.<SampleRecord>builder()
            .addMapping(SampleRecord.class, anyPattern)
            .parseErrorStrategy(ParseErrorStrategy.FORWARD_TO_HANDLER)
            .build()
    );
    assertTrue(ex.getMessage().contains("parseErrorHandler"));
  }

  @Test
  void builderIsFluentReturningItself() {
    FixedFormatReader.Builder<SampleRecord> builder = FixedFormatReader.builder();
    assertSame(builder, builder.addMapping(SampleRecord.class, anyPattern));
    assertSame(builder, builder.multiMatchStrategy(MultiMatchStrategy.FIRST_MATCH));
    assertSame(builder, builder.unmatchedLineStrategy(UnmatchedLineStrategy.SKIP));
    assertSame(builder, builder.parseErrorStrategy(ParseErrorStrategy.THROW));
  }
}
