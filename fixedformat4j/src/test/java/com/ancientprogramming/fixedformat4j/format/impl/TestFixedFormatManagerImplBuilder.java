package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TestFixedFormatManagerImplBuilder {

  // --- Test fixture records ---

  @Record
  static class StringRecord {
    private String value;
    @Field(offset = 1, length = 10)
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
  }

  @Record
  static class UUIDRecord {
    private UUID id;
    @Field(offset = 1, length = 36)
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
  }

  @Record
  static class RepeatingUUIDRecord {
    private UUID[] ids;
    @Field(offset = 1, length = 36, count = 2, align = Align.LEFT, paddingChar = ' ')
    public UUID[] getIds() { return ids; }
    public void setIds(UUID[] ids) { this.ids = ids; }
  }

  // --- Builder shape tests ---

  @Test
  void builder_returnsNonNull() {
    assertNotNull(FixedFormatManagerImpl.builder());
  }

  @Test
  void build_withNoRegistrations_functionalManagerForBuiltInTypes() {
    FixedFormatManager mgr = FixedFormatManagerImpl.builder().build();
    StringRecord rec = mgr.load(StringRecord.class, "hello     ");
    assertEquals("hello", rec.getValue());
  }

  @Test
  void registerType_isFluentReturnsSameBuilder() {
    FixedFormatManagerImpl.Builder b = FixedFormatManagerImpl.builder();
    assertSame(b, b.registerType(UUID.class, TestByTypeFormatter.UUIDFormatter.class));
  }

  // --- Custom unknown type ---

  @Test
  void registerType_customUnknownType_loadWorks() {
    FixedFormatManager mgr = FixedFormatManagerImpl.builder()
        .registerType(UUID.class, TestByTypeFormatter.UUIDFormatter.class)
        .build();
    UUID expected = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    UUIDRecord rec = mgr.load(UUIDRecord.class, "550e8400-e29b-41d4-a716-446655440000");
    assertEquals(expected, rec.getId());
  }

  @Test
  void registerType_customUnknownType_exportWorks() {
    FixedFormatManager mgr = FixedFormatManagerImpl.builder()
        .registerType(UUID.class, TestByTypeFormatter.UUIDFormatter.class)
        .build();
    UUIDRecord rec = new UUIDRecord();
    UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    rec.setId(id);
    assertEquals("550e8400-e29b-41d4-a716-446655440000", mgr.export(rec));
  }

  // --- Overwrite semantics (a): custom formatter shadows built-in ---

  @Test
  void registerType_builtInType_customFormatterShadowsBuiltIn() {
    FixedFormatManager mgr = FixedFormatManagerImpl.builder()
        .registerType(String.class, TestByTypeFormatter.UppercaseStringFormatter.class)
        .build();
    StringRecord rec = mgr.load(StringRecord.class, "hello     ");
    assertEquals("HELLO", rec.getValue());
  }

  // --- Overwrite semantics (b): last-writer-wins ---

  @Test
  void registerTypeTwice_differentFormatters_lastWins() {
    FixedFormatManager mgr = FixedFormatManagerImpl.builder()
        .registerType(String.class, TestByTypeFormatter.UppercaseStringFormatter.class)
        .registerType(String.class, StringFormatter.class)
        .build();
    StringRecord rec = mgr.load(StringRecord.class, "hello     ");
    assertEquals("hello", rec.getValue());
  }

  @Test
  void registerTypeTwice_sameFormatter_isIdempotentNoException() {
    assertDoesNotThrow(() -> FixedFormatManagerImpl.builder()
        .registerType(UUID.class, TestByTypeFormatter.UUIDFormatter.class)
        .registerType(UUID.class, TestByTypeFormatter.UUIDFormatter.class)
        .build());
  }

  // --- Overwrite semantics (c): create() is unaffected ---

  @Test
  void createFactory_unaffectedByBuilderRegistrations() {
    FixedFormatManagerImpl.builder()
        .registerType(String.class, TestByTypeFormatter.UppercaseStringFormatter.class)
        .build();
    FixedFormatManager defaultMgr = FixedFormatManagerImpl.create();
    StringRecord rec = defaultMgr.load(StringRecord.class, "hello     ");
    assertEquals("hello", rec.getValue());
  }

  // --- Repeating field with custom type ---

  @Test
  void builderManager_repeatingFieldCustomType_works() {
    FixedFormatManager mgr = FixedFormatManagerImpl.builder()
        .registerType(UUID.class, TestByTypeFormatter.UUIDFormatter.class)
        .build();
    UUID u1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    UUID u2 = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
    String data = u1.toString() + u2.toString();
    RepeatingUUIDRecord rec = mgr.load(RepeatingUUIDRecord.class, data);
    assertArrayEquals(new UUID[]{u1, u2}, rec.getIds());
  }

  // --- Concurrency ---

  @Test
  void builderManager_concurrentLoadExport_noDataRace() throws Exception {
    FixedFormatManager mgr = FixedFormatManagerImpl.builder()
        .registerType(UUID.class, TestByTypeFormatter.UUIDFormatter.class)
        .build();
    UUID id = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    UUIDRecord template = new UUIDRecord();
    template.setId(id);

    int threads = 8;
    CyclicBarrier barrier = new CyclicBarrier(threads);
    ExecutorService pool = Executors.newFixedThreadPool(threads);
    List<Future<Boolean>> futures = new ArrayList<>();

    for (int i = 0; i < threads; i++) {
      futures.add(pool.submit(() -> {
        barrier.await();
        for (int j = 0; j < 200; j++) {
          String exported = mgr.export(template);
          UUIDRecord loaded = mgr.load(UUIDRecord.class, exported);
          if (!id.equals(loaded.getId())) return false;
        }
        return true;
      }));
    }

    pool.shutdown();
    assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
    for (Future<Boolean> f : futures) {
      assertTrue(f.get(), "concurrent load/export produced inconsistent result");
    }
  }
}
