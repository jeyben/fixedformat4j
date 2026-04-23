package com.ancientprogramming.fixedformat4j.io;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderOutputShapes {

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  private FixedFormatReader multiTypeReader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();
  }

  @Test
  void readAsTypedResultGroupsByMatchedClass() {
    TypedReadResult result = multiTypeReader().readAsTypedResult(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB\nAAAAAAAAAAAA"));

    assertEquals(2, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsTypedResultPreservesRegistrationOrder() {
    TypedReadResult result = multiTypeReader().readAsTypedResult(
        new StringReader("BBBBBBBBBB\nAAAAAAAAAAAA"));

    List<Class<?>> keys = new ArrayList<>(result.classes());
    assertEquals(TenCharRecord.class, keys.get(0));
    assertEquals(FiveCharRecord.class, keys.get(1));
  }

  @Test
  void readAsTypedResultExcludesClassesWithNoMatches() {
    TypedReadResult result = multiTypeReader().readAsTypedResult(
        new StringReader("AAAAAAAAAA"));

    assertTrue(result.contains(TenCharRecord.class));
    assertFalse(result.contains(FiveCharRecord.class));
  }

  @Test
  void readAsListReturnsAllRecordsInEncounterOrder() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .build();

    List<TenCharRecord> results = reader
        .readAsTypedResult(new StringReader("hello     \nworld     "))
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
    assertEquals("hello", results.get(0).getValue());
    assertEquals("world", results.get(1).getValue());
  }

  @Test
  void getAllReturnsFlatListInEncounterOrder() {
    TypedReadResult result = multiTypeReader().readAsTypedResult(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    List<Object> all = result.getAll();
    assertEquals(2, all.size());
    assertInstanceOf(TenCharRecord.class, all.get(0));
    assertInstanceOf(FiveCharRecord.class, all.get(1));
  }
}
