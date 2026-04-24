package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.RegexLinePattern;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.RecordMapping;

class TestRecordMapping {

  @Record(length = 10)
  static class ValidRecord {
    private String data;

    @Field(offset = 1, length = 10)
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
  }

  static class NotARecord {}

  private final LinePattern anyPattern = new RegexLinePattern(".*");

  @Test
  void returnsRecordClass() {
    RecordMapping<ValidRecord> mapping = new RecordMapping<>(ValidRecord.class, anyPattern);
    assertSame(ValidRecord.class, mapping.getRecordClass());
  }

  @Test
  void returnsPattern() {
    RecordMapping<ValidRecord> mapping = new RecordMapping<>(ValidRecord.class, anyPattern);
    assertSame(anyPattern, mapping.getPattern());
  }

  @Test
  void throwsWhenClassNotAnnotatedWithRecord() {
    IllegalArgumentException ex = assertThrows(
        IllegalArgumentException.class,
        () -> new RecordMapping<>(NotARecord.class, anyPattern)
    );
    assertTrue(ex.getMessage().contains("NotARecord"));
  }

  @Test
  void throwsIllegalArgumentWhenClassIsNull() {
    assertThrows(IllegalArgumentException.class,
        () -> new RecordMapping<>(null, anyPattern));
  }

  @Test
  void throwsIllegalArgumentWhenPatternIsNull() {
    assertThrows(IllegalArgumentException.class,
        () -> new RecordMapping<>(ValidRecord.class, null));
  }

  @Test
  void getHandlerReturnsNullWhenConstructedWithTwoArgs() {
    RecordMapping<ValidRecord> mapping = new RecordMapping<>(ValidRecord.class, anyPattern);
    assertNull(mapping.getHandler());
  }

  @Test
  void getHandlerReturnsProvidedConsumerWhenConstructedWithThreeArgs() {
    java.util.function.Consumer<ValidRecord> handler = r -> {};
    RecordMapping<ValidRecord> mapping = new RecordMapping<>(ValidRecord.class, anyPattern, handler);
    assertSame(handler, mapping.getHandler());
  }
}
