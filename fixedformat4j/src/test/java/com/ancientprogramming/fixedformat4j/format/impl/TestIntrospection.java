/*
 * Copyright 2004 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ancientprogramming.fixedformat4j.format.impl;

import com.ancientprogramming.fixedformat4j.annotation.Align;
import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FieldInfo;
import com.ancientprogramming.fixedformat4j.format.FixedFormatIntrospector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The introspection API exposes the field layout of a {@code @Record} class as immutable
 * {@link FieldInfo} descriptors, so documentation generators and format-drift tooling can query
 * the schema instead of re-implementing annotation scanning.
 */
class TestIntrospection {

  private final FixedFormatIntrospector introspector = new FixedFormatManagerImpl();

  @com.ancientprogramming.fixedformat4j.annotation.Record
  public static class BasicRecord {

    @Field(offset = 1, length = 10)
    public String getCustomerId() {
      return null;
    }

    public void setCustomerId(String value) {
    }

    @Field(offset = 11, length = 5)
    public Integer getAmount() {
      return null;
    }

    public void setAmount(Integer value) {
    }
  }

  @Test
  void exposesPropertyNameOffsetLengthAndDataTypePerField() {
    List<FieldInfo> fields = introspector.introspect(BasicRecord.class);

    assertEquals(2, fields.size());

    FieldInfo customerId = fields.get(0);
    assertEquals("customerId", customerId.getPropertyName());
    assertEquals(1, customerId.getOffset());
    assertEquals(10, customerId.getLength());
    assertEquals(String.class, customerId.getDataType());

    FieldInfo amount = fields.get(1);
    assertEquals("amount", amount.getPropertyName());
    assertEquals(11, amount.getOffset());
    assertEquals(5, amount.getLength());
    assertEquals(Integer.class, amount.getDataType());
  }

  @Test
  void defaultsAreReflectedInFieldInfo() {
    FieldInfo customerId = introspector.introspect(BasicRecord.class).get(0);

    assertEquals(Align.LEFT, customerId.getEffectiveAlignment());
    assertEquals(' ', customerId.getPaddingChar());
    assertEquals(Field.UNSET_NULL_CHAR, customerId.getNullChar());
    assertEquals("", customerId.getNullValue());
    assertEquals(1, customerId.getRepeatCount());
    assertEquals(false, customerId.isNestedRecord());
  }

  @Test
  void nonRecordClassIsRejected() {
    assertThrows(FixedFormatException.class, () -> introspector.introspect(String.class));
  }

  @com.ancientprogramming.fixedformat4j.annotation.Record(align = com.ancientprogramming.fixedformat4j.annotation.RecordAlign.RIGHT)
  public static class RightAlignedRecord {

    @Field(offset = 1, length = 8)
    public String getInherited() {
      return null;
    }

    public void setInherited(String value) {
    }

    @Field(offset = 9, length = 8, align = Align.LEFT)
    public String getOverridden() {
      return null;
    }

    public void setOverridden(String value) {
    }
  }

  @Test
  void effectiveAlignmentResolvesRecordDefaultAndFieldOverride() {
    List<FieldInfo> fields = introspector.introspect(RightAlignedRecord.class);

    assertEquals(Align.RIGHT, fields.get(0).getEffectiveAlignment(),
        "field without explicit align inherits @Record(align = RIGHT)");
    assertEquals(Align.LEFT, fields.get(1).getEffectiveAlignment(),
        "explicit @Field(align) overrides the record default");
  }

  @com.ancientprogramming.fixedformat4j.annotation.Record
  public static class ConfiguredRecord {

    @Field(offset = 1, length = 6, paddingChar = '0', nullValue = "999999")
    public Integer getAmount() {
      return null;
    }

    public void setAmount(Integer value) {
    }

    @Field(offset = 7, length = 4, nullChar = ' ',
        formatter = com.ancientprogramming.fixedformat4j.format.impl.StringFormatter.class)
    public String getCode() {
      return null;
    }

    public void setCode(String value) {
    }

    @Field(offset = 11, length = 3, count = 4)
    public List<String> getTags() {
      return null;
    }

    public void setTags(List<String> value) {
    }

    @Field(offset = 23, length = 10)
    public BasicRecord getNested() {
      return null;
    }

    public void setNested(BasicRecord value) {
    }
  }

  @Test
  void sentinelsFormatterRepeatCountAndNestedRecordAreExposed() {
    List<FieldInfo> fields = introspector.introspect(ConfiguredRecord.class);

    FieldInfo amount = fields.get(0);
    assertEquals('0', amount.getPaddingChar());
    assertEquals("999999", amount.getNullValue());

    FieldInfo code = fields.get(1);
    assertEquals(' ', code.getNullChar());
    assertEquals(StringFormatter.class, code.getFormatterClass());

    FieldInfo tags = fields.get(2);
    assertEquals(4, tags.getRepeatCount());
    assertEquals(List.class, tags.getDataType());

    FieldInfo nested = fields.get(3);
    assertEquals(true, nested.isNestedRecord());
    assertEquals(BasicRecord.class, nested.getDataType());
  }

  @com.ancientprogramming.fixedformat4j.annotation.Record
  public static class MultiFormatRecord {

    @com.ancientprogramming.fixedformat4j.annotation.Fields({
        @Field(offset = 1, length = 10),
        @Field(offset = 11, length = 10, align = Align.RIGHT, paddingChar = '0')
    })
    public String getValue() {
      return null;
    }

    public void setValue(String value) {
    }
  }

  @Test
  void fieldsAnnotationYieldsOneFieldInfoPerInnerField() {
    List<FieldInfo> fields = introspector.introspect(MultiFormatRecord.class);

    assertEquals(2, fields.size());
    assertEquals("value", fields.get(0).getPropertyName());
    assertEquals("value", fields.get(1).getPropertyName());
    assertEquals(1, fields.get(0).getOffset());
    assertEquals(11, fields.get(1).getOffset());
    assertEquals('0', fields.get(1).getPaddingChar());
    assertEquals(Align.RIGHT, fields.get(1).getEffectiveAlignment());
  }

  @com.ancientprogramming.fixedformat4j.annotation.Record
  public static class BadPatternRecord {

    @Field(offset = 1, length = 18)
    @com.ancientprogramming.fixedformat4j.annotation.FixedFormatPattern("not-a-date-pattern")
    public java.util.Date getCreatedAt() {
      return null;
    }

    public void setCreatedAt(java.util.Date value) {
    }
  }

  @Test
  void invalidConfigurationFailsAtIntrospectTimeAsAPreflightCheck() {
    assertThrows(FixedFormatException.class, () -> introspector.introspect(BadPatternRecord.class));
  }
}
