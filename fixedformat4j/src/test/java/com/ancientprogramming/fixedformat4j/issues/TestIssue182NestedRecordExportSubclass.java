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
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.FixedFormatManager;
import com.ancientprogramming.fixedformat4j.format.impl.FixedFormatManagerImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Issue 182 (bug #5) - exporting a nested {@code @Record} field whose runtime value is
 * an instance of a subclass that does not itself carry {@code @Record} (it is not
 * {@code @Inherited}) must fail loudly with a {@link FixedFormatException} instead of silently
 * discarding the real data and writing padding.
 *
 * @since 2.0.0
 */
public class TestIssue182NestedRecordExportSubclass {

  private final FixedFormatManager manager = FixedFormatManagerImpl.create();

  @Test
  public void testExportNullNestedRecordStillProducesPadding() {
    Parent182 parent = new Parent182();
    parent.setPrefix("ABC");
    parent.setChild(null);

    assertEquals("ABC          ", manager.export(parent));
  }

  @Test
  public void testExportAnnotatedNestedRecordStillWorks() {
    Child182 child = new Child182();
    child.setValue("foo");
    Parent182 parent = new Parent182();
    parent.setPrefix("ABC");
    parent.setChild(child);

    assertEquals("ABCfoo       ", manager.export(parent));
  }

  @Test
  public void testExportUnannotatedSubclassOfNestedRecordThrowsInsteadOfBlanking() {
    // ChildSubclassWithoutRecord182 carries real data ("foo") but, since @Record is not
    // @Inherited, its class has no @Record annotation of its own.
    ChildSubclassWithoutRecord182 child = new ChildSubclassWithoutRecord182();
    child.setValue("foo");
    Parent182 parent = new Parent182();
    parent.setPrefix("ABC");
    parent.setChild(child);

    FixedFormatException ex = assertThrows(FixedFormatException.class, () -> manager.export(parent));
    assertTrue(ex.getMessage().contains(ChildSubclassWithoutRecord182.class.getName()));
  }

  @Record(length = 13)
  public static class Parent182 {

    private String prefix;
    private Child182 child;

    @Field(offset = 1, length = 3)
    public String getPrefix() {
      return prefix;
    }

    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }

    @Field(offset = 4, length = 10)
    public Child182 getChild() {
      return child;
    }

    public void setChild(Child182 child) {
      this.child = child;
    }
  }

  @Record(length = 10)
  public static class Child182 {

    private String value;

    @Field(offset = 1, length = 10)
    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  public static class ChildSubclassWithoutRecord182 extends Child182 {
  }
}
