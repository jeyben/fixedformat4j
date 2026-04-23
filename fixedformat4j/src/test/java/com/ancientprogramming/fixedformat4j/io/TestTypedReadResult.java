package com.ancientprogramming.fixedformat4j.io;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class TestTypedReadResult {

  private static TenCharRecord tenChar(String value) {
    TenCharRecord r = new TenCharRecord();
    r.setValue(value);
    return r;
  }

  private static FiveCharRecord fiveChar(String code) {
    FiveCharRecord r = new FiveCharRecord();
    r.setCode(code);
    return r;
  }

  @Test
  void getReturnsTypedListForRegisteredClass() {
    TenCharRecord record = tenChar("hello     ");
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    data.put(TenCharRecord.class, List.of(record));
    TypedReadResult result = new TypedReadResult(data, List.of(record));

    List<TenCharRecord> records = result.get(TenCharRecord.class);

    assertEquals(1, records.size());
    assertSame(record, records.get(0));
  }

  @Test
  void getReturnsEmptyListForUnregisteredClass() {
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    TypedReadResult result = new TypedReadResult(data, List.of());

    List<TenCharRecord> records = result.get(TenCharRecord.class);

    assertTrue(records.isEmpty());
  }

  @Test
  void getAllReturnsFlatListInEncounterOrder() {
    TenCharRecord ten = tenChar("hello     ");
    FiveCharRecord five = fiveChar("world");
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    data.put(TenCharRecord.class, List.of(ten));
    data.put(FiveCharRecord.class, List.of(five));
    TypedReadResult result = new TypedReadResult(data, List.of(ten, five));

    List<Object> all = result.getAll();

    assertEquals(List.of(ten, five), all);
  }

  @Test
  void containsReturnsTrueForClassWithRecords() {
    TenCharRecord record = tenChar("hello     ");
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    data.put(TenCharRecord.class, List.of(record));
    TypedReadResult result = new TypedReadResult(data, List.of(record));

    assertTrue(result.contains(TenCharRecord.class));
    assertFalse(result.contains(FiveCharRecord.class));
  }

  @Test
  void classesReturnsOnlyClassesThatProducedRecords() {
    TenCharRecord record = tenChar("hello     ");
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    data.put(TenCharRecord.class, List.of(record));
    TypedReadResult result = new TypedReadResult(data, List.of(record));

    assertEquals(1, result.classes().size());
    assertTrue(result.classes().contains(TenCharRecord.class));
    assertFalse(result.classes().contains(FiveCharRecord.class));
  }
}
