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
}
