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
 * Verifies Issue 45 - nested @Record field without a custom formatter should load/export correctly,
 * and exporting a null nested record should produce padding rather than throw an exception.
 *
 * @since 1.7.1
 */
public class TestIssue45 {

  private final FixedFormatManager manager = new FixedFormatManagerImpl();

  @Test
  public void testLoadNestedRecord() {
    ParentRecord45 parent = manager.load(ParentRecord45.class, "ABCfoo       ");
    assertEquals("ABC", parent.getPrefix());
    assertEquals("foo", parent.getChild().getValue().trim());
  }

  @Test
  public void testExportNestedRecord() {
    ChildRecord45 child = new ChildRecord45();
    child.setValue("foo");

    ParentRecord45 parent = new ParentRecord45();
    parent.setPrefix("ABC");
    parent.setChild(child);

    assertEquals("ABCfoo       ", manager.export(parent));
  }

  @Test
  public void testExportNullNestedRecord() {
    ParentRecord45 parent = new ParentRecord45();
    parent.setPrefix("ABC");
    parent.setChild(null);

    assertEquals("ABC          ", manager.export(parent));
  }

  @Record
  public static class ParentRecord45 {

    private String prefix;
    private ChildRecord45 child;

    @Field(offset = 1, length = 3)
    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    @Field(offset = 4, length = 10)
    public ChildRecord45 getChild() {
      return child;
    }

    public void setChild(ChildRecord45 child) {
      this.child = child;
    }
  }

  @Record
  public static class ChildRecord45 {

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
