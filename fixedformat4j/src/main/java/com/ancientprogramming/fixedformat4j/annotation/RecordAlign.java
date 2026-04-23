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
package com.ancientprogramming.fixedformat4j.annotation;

/**
 * Record-level alignment choices for {@link Record#align()}.
 *
 * <p>Unlike {@link Align}, this enum contains only the two meaningful values for a record:
 * {@link #LEFT} and {@link #RIGHT}. The {@code INHERIT} sentinel from {@link Align} has no
 * meaning at the record level and is therefore absent here, making misconfiguration
 * impossible at compile time.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public enum RecordAlign {

  /**
   * Pad or chop data to the right so the text is aligned to the left.
   * This is the default for {@link Record#align()}.
   */
  LEFT,

  /**
   * Pad or chop data to the left so the text is aligned to the right.
   */
  RIGHT
}
