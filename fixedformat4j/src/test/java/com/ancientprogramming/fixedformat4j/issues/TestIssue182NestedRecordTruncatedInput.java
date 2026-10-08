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
package com.ancientprogramming.fixedformat4j.issues;

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.annotation.Record;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies Issue 182 (bug #2) - loading a record whose data string is truncated before a nested
 * {@code @Record} field even starts must degrade the nested field to {@code null}, the same way
 * every other field type already does, instead of throwing a {@code NullPointerException}.
 *
 * @since 2.0.0
 */
public class TestIssue182NestedRecordTruncatedInput {

  private final FixedFormatManager manager = FixedFormatManagerImpl.create();

  @Test
  public void testLoadNestedRecordFromTruncatedData() {
    // "abcde" is only 5 chars long - the nested Inner field starts at offset 6 and is never reached.
    Outer182 outer = manager.load(Outer182.class, "abcde");

    assertEquals("abcde", outer.getPrefix());
    assertNull(outer.getInner());
  }

  @Record(length = 15)
  public static class Outer182 {

    private String prefix;
    private Inner182 inner;

    @Field(offset = 1, length = 5)
    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    @Field(offset = 6, length = 10)
    public Inner182 getInner() {
      return inner;
    }

    public void setInner(Inner182 inner) {
      this.inner = inner;
    }
  }

  @Record(length = 10)
  public static class Inner182 {

    private String value;

    @Field(offset = 1, length = 10)
    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }
}
