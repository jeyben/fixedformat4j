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
 * Supplementary annotation to control how an enum field is serialized in a fixed-width record.
 * Place this annotation on a getter method alongside {@link Field} to override the default
 * {@link EnumFormat#LITERAL} serialization.
 *
 * <p>If this annotation is omitted, the enum field defaults to {@link EnumFormat#LITERAL}.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface FixedFormatEnum {

  /**
   * The serialization format for the enum field.
   *
   * @return the {@link EnumFormat} to use; defaults to {@link EnumFormat#LITERAL}
   */
  EnumFormat value() default EnumFormat.LITERAL;
}
