package com.ancientprogramming.fixedformat4j.io;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatReaderOutputShapes {

  private static final FixedFormatMatchPattern A_PATTERN = new RegexFixedFormatMatchPattern("^A");
  private static final FixedFormatMatchPattern B_PATTERN = new RegexFixedFormatMatchPattern("^B");

  private FixedFormatReader<Object> multiTypeReader() {
    return FixedFormatReader.<Object>builder()
        .addMapping(TenCharRecord.class, A_PATTERN)
        .addMapping(FiveCharRecord.class, B_PATTERN)
        .build();
  }

  @Test
  void readAsListReturnsAllRecordsInEncounterOrder() {
    FixedFormatReader<TenCharRecord> reader = FixedFormatReader.<TenCharRecord>builder()
        .addMapping(TenCharRecord.class, new RegexFixedFormatMatchPattern(".*"))
        .build();

    List<TenCharRecord> results = reader.readAsList(new StringReader("hello     \nworld     "));
    assertEquals(2, results.size());
    assertEquals("hello", results.get(0).getValue());
    assertEquals("world", results.get(1).getValue());
  }

  @Test
  void readAsMapGroupsByMatchedClass() {
    Map<Class<? extends Object>, List<Object>> map =
        multiTypeReader().readAsMap(new StringReader("AAAAAAAAAA\nBBBBBBBBBB\nAAAAAAAAAAAA"));

    assertEquals(2, map.size());
    assertEquals(2, map.get(TenCharRecord.class).size());
    assertEquals(1, map.get(FiveCharRecord.class).size());
  }

  @Test
  void readAsMapPreservesRegistrationOrder() {
    Map<Class<? extends Object>, List<Object>> map =
        multiTypeReader().readAsMap(new StringReader("BBBBBBBBBB\nAAAAAAAAAAAA"));

    List<Class<?>> keys = new ArrayList<>(map.keySet());
    assertEquals(TenCharRecord.class, keys.get(0));
    assertEquals(FiveCharRecord.class, keys.get(1));
  }

  @Test
  void readAsMapExcludesClassesWithNoMatches() {
    Map<Class<? extends Object>, List<Object>> map =
        multiTypeReader().readAsMap(new StringReader("AAAAAAAAAA"));

    assertEquals(1, map.size());
    assertTrue(map.containsKey(TenCharRecord.class));
    assertFalse(map.containsKey(FiveCharRecord.class));
  }
}
