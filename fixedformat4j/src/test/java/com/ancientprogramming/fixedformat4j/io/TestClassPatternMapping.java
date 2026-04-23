package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestClassPatternMapping {

  @Record(length = 10)
  static class ValidRecord {
    private String data;

    @Field(offset = 1, length = 10)
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
  }

  static class NotARecord {}

  private final FixedFormatMatchPattern anyPattern = new RegexFixedFormatMatchPattern(".*");

  @Test
  void returnsRecordClass() {
    ClassPatternMapping<ValidRecord> mapping = new ClassPatternMapping<>(ValidRecord.class, anyPattern);
    assertSame(ValidRecord.class, mapping.getRecordClass());
  }

  @Test
  void returnsPattern() {
    ClassPatternMapping<ValidRecord> mapping = new ClassPatternMapping<>(ValidRecord.class, anyPattern);
    assertSame(anyPattern, mapping.getPattern());
  }

  @Test
  void throwsWhenClassNotAnnotatedWithRecord() {
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> new ClassPatternMapping<>(NotARecord.class, anyPattern)
    );
    assertTrue(ex.getMessage().contains("NotARecord"));
  }

  @Test
  void throwsIllegalArgumentWhenClassIsNull() {
    assertThrows(IllegalArgumentException.class,
        () -> new ClassPatternMapping<>(null, anyPattern));
  }

  @Test
  void throwsIllegalArgumentWhenPatternIsNull() {
    assertThrows(IllegalArgumentException.class,
        () -> new ClassPatternMapping<>(ValidRecord.class, null));
  }

  @Test
  void getHandlerReturnsNullWhenConstructedWithTwoArgs() {
    ClassPatternMapping<ValidRecord> mapping = new ClassPatternMapping<>(ValidRecord.class, anyPattern);
    assertNull(mapping.getHandler());
  }

  @Test
  void getHandlerReturnsProvidedConsumerWhenConstructedWithThreeArgs() {
    java.util.function.Consumer<ValidRecord> handler = r -> {};
    ClassPatternMapping<ValidRecord> mapping = new ClassPatternMapping<>(ValidRecord.class, anyPattern, handler);
    assertSame(handler, mapping.getHandler());
  }
}
