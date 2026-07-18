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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies Issue 182 (bug #6) - a self-referential {@code @Record} graph (a record nesting a
 * field of its own type at the same width) must fail fast with a {@link FixedFormatException}
 * instead of recursing until a {@code StackOverflowError}.
 *
 * @since 2.0.0
 */
public class TestIssue182SelfReferentialRecord {

  private final FixedFormatManager manager = FixedFormatManagerImpl.create();

  @Test
  public void testLoadSelfReferentialRecordFailsFastInsteadOfOverflowing() {
    // The single field is the same width as the record itself, so without a recursion guard
    // load() would recurse with an identical-length string forever.
    FixedFormatException ex = assertThrows(FixedFormatException.class,
        () -> manager.load(SelfReferential182.class, "1234567890"));
    assertTrue(ex.getMessage().contains(SelfReferential182.class.getName()));
  }

  @Test
  public void testExportSelfReferentialRecordFailsFastInsteadOfOverflowing() {
    SelfReferential182 record = new SelfReferential182();
    record.setSelf(record);

    FixedFormatException ex = assertThrows(FixedFormatException.class, () -> manager.export(record));
    assertTrue(ex.getMessage().contains(SelfReferential182.class.getName()));
  }

  @Record(length = 10)
  public static class SelfReferential182 {

    private SelfReferential182 self;

    @Field(offset = 1, length = 10)
    public SelfReferential182 getSelf() {
      return self;
    }

    public void setSelf(SelfReferential182 self) {
      this.self = self;
    }
  }
}
