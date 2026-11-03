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
 * Issue 182 (finding #6) - a {@code @Record} that nests its own type is legitimate as long as the
 * chain ends in {@code null}. Guards against a class-based recursion check, which would reject the
 * second level of such a chain even though it terminates.
 *
 * @since 2.0.0
 */
public class TestIssue182SelfReferentialRecord {

  private final FixedFormatManager manager = FixedFormatManagerImpl.create();

  @Test
  public void testExportSelfTypedChainEndingInNull() {
    assertEquals("a    b         ", manager.export(node("a", node("b", null))));
  }

  @Test
  public void testLoadSelfTypedChainEndsWhenDataRunsOut() {
    Node182 loaded = manager.load(Node182.class, "a    b         ");

    assertEquals("a", loaded.getValue());
    assertEquals("b", loaded.getNext().getValue());
    assertNull(loaded.getNext().getNext());
  }

  private static Node182 node(String value, Node182 next) {
    Node182 node = new Node182();
    node.setValue(value);
    node.setNext(next);
    return node;
  }

  @Record
  public static class Node182 {

    private String value;
    private Node182 next;

    @Field(offset = 1, length = 5)
    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }

    @Field(offset = 6, length = 5)
    public Node182 getNext() {
      return next;
    }

    public void setNext(Node182 next) {
      this.next = next;
    }
  }
}
