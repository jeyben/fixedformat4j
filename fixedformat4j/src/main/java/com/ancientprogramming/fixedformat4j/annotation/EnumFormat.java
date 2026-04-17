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
 * Defines how an enum field is serialized to/from a fixed-width string.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.0
 */
public enum EnumFormat {

  /**
   * Serializes using {@link Enum#name()} and deserializes using {@link Enum#valueOf(Class, String)}.
   * This is the default when no {@code @FixedFormatEnum} annotation is present.
   */
  LITERAL,

  /**
   * Serializes using {@link Enum#ordinal()} and deserializes by index lookup into
   * {@link Class#getEnumConstants()}.
   */
  NUMERIC
}
