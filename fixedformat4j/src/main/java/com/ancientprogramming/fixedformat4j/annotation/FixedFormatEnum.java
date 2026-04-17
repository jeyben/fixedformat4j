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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Supplementary annotation for enum fields that controls whether the enum is serialized as its
 * name ({@link EnumFormat#LITERAL}) or its ordinal ({@link EnumFormat#NUMERIC}).
 *
 * <p>When omitted, enum fields default to {@link EnumFormat#LITERAL}.
 *
 * <p>Example:
 * <pre>
 *   &#64;Field(offset = 1, length = 10)
 *   &#64;FixedFormatEnum(EnumFormat.NUMERIC)
 *   public Status getStatus() { ... }
 * </pre>
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface FixedFormatEnum {

  /**
   * The serialization format. Defaults to {@link EnumFormat#LITERAL}.
   *
   * @return the enum format to use for this field
   */
  EnumFormat value() default EnumFormat.LITERAL;
}
