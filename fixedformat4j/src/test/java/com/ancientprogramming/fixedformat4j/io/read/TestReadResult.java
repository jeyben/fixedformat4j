package com.ancientprogramming.fixedformat4j.io.read;

import com.ancientprogramming.fixedformat4j.io.FiveCharRecord;
import com.ancientprogramming.fixedformat4j.io.TenCharRecord;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TestReadResult {

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
    ReadResult result = new ReadResult(data, List.of(record));

    List<TenCharRecord> records = result.get(TenCharRecord.class);

    assertEquals(1, records.size());
    assertSame(record, records.get(0));
  }

  @Test
  void getReturnsEmptyListForUnregisteredClass() {
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    ReadResult result = new ReadResult(data, List.of());

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
    ReadResult result = new ReadResult(data, List.of(ten, five));

    List<Object> all = result.getAll();

    assertEquals(List.of(ten, five), all);
  }

  @Test
  void containsReturnsTrueForClassWithRecords() {
    TenCharRecord record = tenChar("hello     ");
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    data.put(TenCharRecord.class, List.of(record));
    ReadResult result = new ReadResult(data, List.of(record));

    assertTrue(result.contains(TenCharRecord.class));
    assertFalse(result.contains(FiveCharRecord.class));
  }

  @Test
  void getReturnedListIsUnmodifiable() {
    TenCharRecord record = tenChar("hello     ");
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    data.put(TenCharRecord.class, new ArrayList<>(List.of(record)));
    ReadResult result = new ReadResult(data, List.of(record));

    List<TenCharRecord> records = result.get(TenCharRecord.class);

    assertThrows(UnsupportedOperationException.class, () -> records.clear());
  }

  @Test
  void classesReturnsOnlyClassesThatProducedRecords() {
    TenCharRecord record = tenChar("hello     ");
    Map<Class<?>, List<Object>> data = new LinkedHashMap<>();
    data.put(TenCharRecord.class, List.of(record));
    ReadResult result = new ReadResult(data, List.of(record));

    assertEquals(1, result.classes().size());
    assertTrue(result.classes().contains(TenCharRecord.class));
    assertFalse(result.classes().contains(FiveCharRecord.class));
  }
}
