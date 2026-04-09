package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Fields;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestAnnotationScanner {

  private final AnnotationScanner scanner = new AnnotationScanner();

  @Test
  void scan_methodAnnotation_returnsTarget() {
    List<AnnotationTarget> targets = scanner.scan(MethodAnnotatedRecord.class);
    assertEquals(1, targets.size());
    assertEquals("getValue", targets.get(0).getter.getName());
    assertNotNull(targets.get(0).annotationSource.getAnnotation(Field.class));
  }

  @Test
  void scan_fieldAnnotation_returnsTarget() {
    List<AnnotationTarget> targets = scanner.scan(FieldAnnotatedRecord.class);
    assertEquals(1, targets.size());
    assertEquals("getValue", targets.get(0).getter.getName());
    assertNotNull(targets.get(0).annotationSource.getAnnotation(Field.class));
  }

  @Test
  void scan_fieldAndMethodAnnotation_fieldTakesPriority() {
    List<AnnotationTarget> targets = scanner.scan(BothAnnotatedRecord.class);
    assertEquals(1, targets.size());
    assertInstanceOf(java.lang.reflect.Field.class, targets.get(0).annotationSource);
  }

  @Test
  void scan_fieldsAnnotation_returnsTarget() {
    List<AnnotationTarget> targets = scanner.scan(FieldsAnnotatedRecord.class);
    assertEquals(1, targets.size());
    assertNotNull(targets.get(0).annotationSource.getAnnotation(Fields.class));
  }

  @Test
  void scan_superclassFields_included() {
    List<AnnotationTarget> targets = scanner.scan(SubRecord.class);
    boolean hasBaseField = targets.stream().anyMatch(t -> t.getter.getName().equals("getBaseValue"));
    assertTrue(hasBaseField, "Expected field from superclass to be included");
  }

  @Test
  void scan_orderPreserved() {
    List<AnnotationTarget> targets = scanner.scan(MultiFieldRecord.class);
    assertEquals(2, targets.size());
    assertEquals("getFirst", targets.get(0).getter.getName());
    assertEquals("getSecond", targets.get(1).getter.getName());
  }

  // --- Fixture classes ---

  @Record
  static class MethodAnnotatedRecord {
    private String value;

    @Field(offset = 1, length = 5)
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
  }

  @Record
  static class FieldAnnotatedRecord {
    @Field(offset = 1, length = 5)
    private String value;

    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
  }

  @Record
  static class BothAnnotatedRecord {
    @Field(offset = 1, length = 5)
    private String value;

    @Field(offset = 1, length = 5)
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
  }

  @Record
  static class FieldsAnnotatedRecord {
    private String value;

    @Fields({@Field(offset = 1, length = 5)})
    public String getValue() { return value; }
    public void setValue(String v) { this.value = v; }
  }

  @Record
  static class BaseRecord {
    @Field(offset = 1, length = 5)
    private String baseValue;

    public String getBaseValue() { return baseValue; }
    public void setBaseValue(String v) { this.baseValue = v; }
  }

  @Record
  static class SubRecord extends BaseRecord {
  }

  @Record
  static class MultiFieldRecord {
    @Field(offset = 1, length = 5)
    private String first;

    @Field(offset = 6, length = 5)
    private String second;

    public String getFirst() { return first; }
    public void setFirst(String v) { this.first = v; }

    public String getSecond() { return second; }
    public void setSecond(String v) { this.second = v; }
  }
}
