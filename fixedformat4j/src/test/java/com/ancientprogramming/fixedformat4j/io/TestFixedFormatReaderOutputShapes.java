package com.ancientprogramming.fixedformat4j.io;

import com.ancientprogramming.fixedformat4j.io.read.LinePattern;
import com.ancientprogramming.fixedformat4j.io.read.ReadResult;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import com.ancientprogramming.fixedformat4j.io.read.FixedFormatReader;

class TestFixedFormatReaderOutputShapes {

  private FixedFormatReader multiTypeReader() {
    return FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.prefix("A"))
        .addMapping(FiveCharRecord.class, LinePattern.prefix("B"))
        .build();
  }

  @Test
  void readGroupsByMatchedClass() {
    ReadResult result = multiTypeReader().read(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB\nAAAAAAAAAAAA"));

    assertEquals(2, result.get(TenCharRecord.class).size());
    assertEquals(1, result.get(FiveCharRecord.class).size());
  }

  @Test
  void readPreservesRegistrationOrder() {
    ReadResult result = multiTypeReader().read(
        new StringReader("BBBBBBBBBB\nAAAAAAAAAAAA"));

    List<Class<?>> keys = new ArrayList<>(result.classes());
    assertEquals(TenCharRecord.class, keys.get(0));
    assertEquals(FiveCharRecord.class, keys.get(1));
  }

  @Test
  void readExcludesClassesWithNoMatches() {
    ReadResult result = multiTypeReader().read(
        new StringReader("AAAAAAAAAA"));

    assertTrue(result.contains(TenCharRecord.class));
    assertFalse(result.contains(FiveCharRecord.class));
  }

  @Test
  void readGetReturnsTypedRecordsInOrder() {
    FixedFormatReader reader = FixedFormatReader.builder()
        .addMapping(TenCharRecord.class, LinePattern.matchAll())
        .build();

    List<TenCharRecord> results = reader
        .read(new StringReader("hello     \nworld     "))
        .get(TenCharRecord.class);
    assertEquals(2, results.size());
    assertEquals("hello", results.get(0).getValue());
    assertEquals("world", results.get(1).getValue());
  }

  @Test
  void getAllReturnsFlatListInEncounterOrder() {
    ReadResult result = multiTypeReader().read(
        new StringReader("AAAAAAAAAA\nBBBBBBBBBB"));

    List<Object> all = result.getAll();
    assertEquals(2, all.size());
    assertInstanceOf(TenCharRecord.class, all.get(0));
    assertInstanceOf(FiveCharRecord.class, all.get(1));
  }
}
