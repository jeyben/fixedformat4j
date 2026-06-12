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

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.FieldInfo;
import com.ancientprogramming.fixedformat4j.format.FixedFormatIntrospector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Introspection of Java {@code record} classes: property names are the component names as-is
 * (no get/is prefix to strip — and stripping would mangle components like {@code issuer}).
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
public class TestIntrospectionJavaRecord {

  private final FixedFormatIntrospector introspector = new FixedFormatManagerImpl();

  @Record
  record CustomerRecord(
      @Field(offset = 1, length = 10) String customerId,
      @Field(offset = 11, length = 20) String issuer) {}

  @Test
  public void recordComponentNamesAreExposedVerbatim() {
    List<FieldInfo> fields = introspector.introspect(CustomerRecord.class);

    assertEquals(2, fields.size());
    assertEquals("customerId", fields.get(0).getPropertyName());
    assertEquals(String.class, fields.get(0).getDataType());
    assertEquals("issuer", fields.get(1).getPropertyName());
    assertEquals(11, fields.get(1).getOffset());
    assertEquals(20, fields.get(1).getLength());
  }
}
