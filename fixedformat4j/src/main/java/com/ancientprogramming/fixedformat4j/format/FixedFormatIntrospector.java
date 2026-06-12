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
package com.ancientprogramming.fixedformat4j.format;

import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;

import java.util.List;

/**
 * Read-only access to the field layout of a {@code @Record} class.
 *
 * <p>Intended for tooling built on top of the annotated schema: documentation generators,
 * UI form builders, format negotiation (detecting offset/length drift between record class
 * versions), and layout assertions in tests.
 *
 * <p>This is a separate interface rather than part of {@link FixedFormatManager} so that
 * existing third-party {@code FixedFormatManager} implementations remain source and binary
 * compatible, and so callers that only need schema access can depend on this narrow contract.
 * {@code FixedFormatManagerImpl} implements both.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.9.0
 */
public interface FixedFormatIntrospector {

  /**
   * Returns one {@link FieldInfo} per effective {@code @Field} annotation of the given record
   * class, ordered by {@link FieldInfo#getOffset() offset}. A getter carrying {@code @Fields}
   * contributes one entry per inner {@code @Field}.
   *
   * <p>Calling this method triggers the same one-time metadata build and annotation validation
   * that {@code load()} / {@code export()} trigger, so it is safe to use as a configuration
   * preflight check at application startup. The returned list is rebuilt on each call from the
   * cached metadata (an O(number of fields) mapping with no I/O); the underlying scan and
   * validation run only once per class.
   *
   * @param clazz the {@code @Record}-annotated class to introspect
   * @return an immutable list of field descriptors ordered by offset; never {@code null}
   * @throws FixedFormatException if {@code clazz} is not annotated with {@code @Record} or its
   *                              annotation configuration is invalid
   */
  List<FieldInfo> introspect(Class<?> clazz);
}
