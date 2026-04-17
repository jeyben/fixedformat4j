package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TestClassMetadataCache {

  @Test
  void cacheReturnsSameListInstanceOnSecondCall() {
    ClassMetadataCache cache = new ClassMetadataCache();
    List<FieldDescriptor> first = cache.get(MyRecord.class);
    List<FieldDescriptor> second = cache.get(MyRecord.class);
    assertSame(first, second);
  }

  @Test
  void myRecordProducesTenDescriptors() {
    ClassMetadataCache cache = new ClassMetadataCache();
    assertEquals(10, cache.get(MyRecord.class).size());
  }

  @Test
  void eachSimpleFieldDescriptorHasNonNullMetadata() {
    ClassMetadataCache cache = new ClassMetadataCache();
    for (FieldDescriptor d : cache.get(MyRecord.class)) {
      String name = d.target.getter.getName();
      assertNotNull(d.target, name + ": target");
      assertNotNull(d.target.getterHandle, name + ": getterHandle");
      assertNotNull(d.setter, name + ": setter");
      assertNotNull(d.setterHandle, name + ": setterHandle");
      assertNotNull(d.fieldAnnotation, name + ": fieldAnnotation");
      assertNotNull(d.datatype, name + ": datatype");
      assertNotNull(d.context, name + ": context");
      assertNotNull(d.formatInstructions, name + ": formatInstructions");
      assertNotNull(d.formatter, name + ": formatter");
    }
  }

  @Test
  void allSimpleFieldDescriptorsAreMarkedAsLoadFields() {
    ClassMetadataCache cache = new ClassMetadataCache();
    for (FieldDescriptor d : cache.get(MyRecord.class)) {
      assertTrue(d.isLoadField, d.target.getter.getName() + " should be a load field");
    }
  }

  @Test
  void fieldsAnnotationExpandsIntoMultipleDescriptors() {
    ClassMetadataCache cache = new ClassMetadataCache();
    List<FieldDescriptor> descriptors = cache.get(MultibleFieldsRecord.class);
    long count = descriptors.stream()
        .filter(d -> d.target.getter.getName().equals("getDateData"))
        .count();
    assertEquals(2, count);
  }

  @Test
  void onlyFirstFieldsAnnotationDescriptorIsLoadField() {
    ClassMetadataCache cache = new ClassMetadataCache();
    List<FieldDescriptor> dateDescriptors = cache.get(MultibleFieldsRecord.class).stream()
        .filter(d -> d.target.getter.getName().equals("getDateData"))
        .collect(Collectors.toList());
    assertEquals(2, dateDescriptors.size());
    assertTrue(dateDescriptors.get(0).isLoadField, "first @Fields descriptor should be load field");
    assertFalse(dateDescriptors.get(1).isLoadField, "second @Fields descriptor should not be load field");
  }

  @Test
  void repeatingFieldDescriptorHasNullContextAndInstructions() {
    ClassMetadataCache cache = new ClassMetadataCache();
    FieldDescriptor repeating = cache.get(RepeatingFieldRecord.class).stream()
        .filter(d -> d.isRepeating)
        .findFirst()
        .orElseThrow(() -> new AssertionError("no repeating descriptor found"));
    assertNull(repeating.context);
    assertNull(repeating.formatInstructions);
    assertNull(repeating.formatter);
  }

  @Test
  void nestedRecordFieldDescriptorHasNullFormatter() {
    ClassMetadataCache cache = new ClassMetadataCache();
    FieldDescriptor nested = cache.get(NestedRecordHolder.class).stream()
        .filter(d -> d.isNestedRecord)
        .findFirst()
        .orElseThrow(() -> new AssertionError("no nested record descriptor found"));
    assertNull(nested.formatter);
    assertNotNull(nested.context);
    assertNotNull(nested.formatInstructions);
  }

  @Test
  void fieldWithCustomFormatterIsNotMarkedAsNestedRecord() {
    ClassMetadataCache cache = new ClassMetadataCache();
    List<FieldDescriptor> descriptors = cache.get(MyOtherRecord.class);
    assertEquals(1, descriptors.size());
    FieldDescriptor d = descriptors.get(0);
    assertFalse(d.isNestedRecord, "custom formatter field should not be isNestedRecord");
    assertNotNull(d.formatter, "custom formatter field should have a cached formatter");
  }

  @Record
  static class NestedRecordHolder {
    private MyRecord inner;

    @Field(offset = 1, length = 70)
    public MyRecord getInner() { return inner; }
    public void setInner(MyRecord inner) { this.inner = inner; }
  }
}
