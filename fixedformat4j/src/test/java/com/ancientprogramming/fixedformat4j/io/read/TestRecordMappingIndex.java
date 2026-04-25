package com.ancientprogramming.fixedformat4j.io.read;

import com.ancientprogramming.fixedformat4j.annotation.Record;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestRecordMappingIndex {

  @Record(length = 10) static class TypeA {}
  @Record(length = 10) static class TypeB {}
  @Record(length = 10) static class TypeC {}

  private static <T> RecordMapping<T> mapping(Class<T> clazz) {
    return new RecordMapping<>(clazz, LinePattern.matchAll());
  }

  @Test
  void prefixHitReturnsMapping() {
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.prefix("HDR"), a)
        .build();
    List<RecordMapping<?>> result = index.findMatches("HDR-data");
    assertEquals(1, result.size());
    assertSame(a, result.get(0));
  }

  @Test
  void prefixMissReturnsEmpty() {
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.prefix("HDR"), a)
        .build();
    assertTrue(index.findMatches("DTL-data").isEmpty());
  }

  @Test
  void positionalHitReturnsMapping() {
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K40001"), a)
        .build();
    List<RecordMapping<?>> result = index.findMatches("K400ABC01x");
    assertEquals(1, result.size());
    assertSame(a, result.get(0));
  }

  @Test
  void positionalMissReturnsEmpty() {
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K40001"), a)
        .build();
    assertTrue(index.findMatches("K400ABC02x").isEmpty());
  }

  @Test
  void lineShorterThanMaxPositionDoesNotMatch() {
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K40001"), a)
        .build();
    assertTrue(index.findMatches("K4000").isEmpty());
  }

  @Test
  void lineLengthExactlyEqualToMaxPositionDoesNotMatch() {
    // line of length 9, max position 8 ⇒ index 8 is reachable; line of length 8 is not
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.positional(new int[]{0, 8}, "AB"), a)
        .build();
    assertTrue(index.findMatches("Axxxxxxx").isEmpty());          // length 8
    assertEquals(1, index.findMatches("AxxxxxxxB").size());        // length 9
  }

  @Test
  void emptyLineMatchesOnlyMatchAll() {
    RecordMapping<TypeA> any = mapping(TypeA.class);
    RecordMapping<TypeB> hdr = mapping(TypeB.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.matchAll(), any)
        .add(LinePattern.prefix("HDR"), hdr)
        .build();
    List<RecordMapping<?>> result = index.findMatches("");
    assertEquals(1, result.size());
    assertSame(any, result.get(0));
  }

  @Test
  void matchAllReturnsMappingForEveryLine() {
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.matchAll(), a)
        .build();
    assertEquals(1, index.findMatches("anything").size());
    assertEquals(1, index.findMatches("").size());
  }

  @Test
  void multipleMappingsSharingSamePatternAreAllReturnedInRegistrationOrder() {
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMapping<TypeB> b = mapping(TypeB.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.prefix("X"), a)
        .add(LinePattern.prefix("X"), b)
        .build();
    List<RecordMapping<?>> result = index.findMatches("Xline");
    assertEquals(2, result.size());
    assertSame(a, result.get(0));
    assertSame(b, result.get(1));
  }

  @Test
  void mostDetailedMatchComesFirst() {
    RecordMapping<TypeA> deeper = mapping(TypeA.class);
    RecordMapping<TypeB> shallower = mapping(TypeB.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.positional(new int[]{0, 1, 2, 3, 7, 8}, "K40001"), deeper)
        .add(LinePattern.positional(new int[]{0, 1, 2, 3, 7}, "K4000"), shallower)
        .build();
    List<RecordMapping<?>> result = index.findMatches("K400ABC01x");
    assertEquals(2, result.size());
    assertSame(deeper, result.get(0));
    assertSame(shallower, result.get(1));
  }

  @Test
  void registrationOrderAppliesAmongSameDepthMatches() {
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMapping<TypeB> b = mapping(TypeB.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.positional(new int[]{0}, "X"), a)
        .add(LinePattern.positional(new int[]{1}, "Y"), b)
        .build();
    List<RecordMapping<?>> result = index.findMatches("XY___");
    assertEquals(2, result.size());
    assertSame(a, result.get(0));
    assertSame(b, result.get(1));
  }

  @Test
  void matchAllAppearsAfterDeeperMatches() {
    RecordMapping<TypeA> any = mapping(TypeA.class);
    RecordMapping<TypeB> hdr = mapping(TypeB.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.matchAll(), any)
        .add(LinePattern.prefix("HDR"), hdr)
        .build();
    List<RecordMapping<?>> result = index.findMatches("HDRdata");
    assertEquals(2, result.size());
    assertSame(hdr, result.get(0));
    assertSame(any, result.get(1));
  }

  @Test
  void disjointSignaturesAreIndexedSeparately() {
    RecordMapping<TypeA> a = mapping(TypeA.class);
    RecordMapping<TypeB> b = mapping(TypeB.class);
    RecordMappingIndex index = RecordMappingIndex.builder()
        .add(LinePattern.prefix("HDR"), a)
        .add(LinePattern.positional(new int[]{5}, "Z"), b)
        .build();
    List<RecordMapping<?>> aOnly = index.findMatches("HDRxxxx");
    assertEquals(1, aOnly.size());
    assertSame(a, aOnly.get(0));

    List<RecordMapping<?>> bOnly = index.findMatches("xxxxxZxx");
    assertEquals(1, bOnly.size());
    assertSame(b, bOnly.get(0));

    List<RecordMapping<?>> both = index.findMatches("HDRxxZxx");
    assertEquals(2, both.size());
    assertSame(a, both.get(0));
    assertSame(b, both.get(1));
  }

  @Test
  void scalesWithManyMappings() {
    RecordMappingIndex.Builder builder = RecordMappingIndex.builder();
    RecordMapping<TypeA> target = mapping(TypeA.class);
    for (int i = 0; i < 1000; i++) {
      String prefix = String.format("P%04d", i);
      builder.add(LinePattern.prefix(prefix), i == 500 ? target : mapping(TypeC.class));
    }
    RecordMappingIndex index = builder.build();
    List<RecordMapping<?>> result = index.findMatches("P0500-line-content");
    assertEquals(1, result.size());
    assertSame(target, result.get(0));
  }

  @Test
  void builderRejectsNullPattern() {
    RecordMappingIndex.Builder builder = RecordMappingIndex.builder();
    assertThrows(NullPointerException.class,
        () -> builder.add(null, mapping(TypeA.class)));
  }

  @Test
  void builderRejectsNullMapping() {
    RecordMappingIndex.Builder builder = RecordMappingIndex.builder();
    assertThrows(NullPointerException.class,
        () -> builder.add(LinePattern.matchAll(), null));
  }
}
