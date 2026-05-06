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
package com.ancientprogramming.fixedformat4j.io.read;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatIOException;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Spliterators;
import java.util.function.Consumer;

import static java.lang.String.format;

class FixedFormatSpliterator extends Spliterators.AbstractSpliterator<Object> {

  private final BufferedReader reader;
  private final FixedFormatLineProcessor processor;
  private final ArrayDeque<Object> buffer = new ArrayDeque<>();
  private long lineNumber = 0L;
  private boolean exhausted = false;

  FixedFormatSpliterator(BufferedReader reader, FixedFormatLineProcessor processor) {
    super(Long.MAX_VALUE, ORDERED | NONNULL);
    this.reader = reader;
    this.processor = processor;
  }

  @Override
  public boolean tryAdvance(Consumer<? super Object> action) {
    while (buffer.isEmpty()) {
      if (exhausted) return false;
      String line;
      try {
        line = reader.readLine();
      } catch (IOException e) {
        throw new FixedFormatIOException(format("IO error reading line %d", lineNumber + 1), e);
      }
      if (line == null) {
        exhausted = true;
        return false;
      }
      processor.processLine(line, ++lineNumber, (clazz, record) -> buffer.add(record));
    }
    action.accept(buffer.poll());
    return true;
  }
}
