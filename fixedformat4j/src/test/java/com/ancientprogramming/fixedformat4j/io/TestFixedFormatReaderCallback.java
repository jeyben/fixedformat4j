package com.ancientprogramming.fixedformat4j.io;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderCallback {

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  @Test
  void consumerCallbackInvokesForEachRecordInOrder() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .build();

    List<String> values = new ArrayList<>();
    reader.readWithCallback(new StringReader("hello     \nworld     "),
        record -> values.add(record.getValue()));

    assertEquals(List.of("hello", "world"), values);
  }

  @Test
  void biConsumerCallbackReceivesMatchedClassAndRecord() {
    FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();

    List<String> captured = new ArrayList<>();
    reader.readWithCallback(new StringReader("AAAAAAAAAA\nBBBBBBBBBB"),
        (clazz, record) -> captured.add(clazz.getSimpleName()));

    assertEquals(List.of("TenCharRecord", "FiveCharRecord"), captured);
  }

  @Test
  void biConsumerCallbackPreservesEncounterOrder() {
    FixedFormatReader<Object> reader = FixedFormatReader.<Object>builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();

    List<Class<?>> classes = new ArrayList<>();
    reader.readWithCallback(new StringReader("BBBBBBBBBB\nAAAAAAAAAAAA"),
        (clazz, record) -> classes.add(clazz));

    assertEquals(FiveCharRecord.class, classes.get(0));
    assertEquals(TenCharRecord.class, classes.get(1));
  }
}
