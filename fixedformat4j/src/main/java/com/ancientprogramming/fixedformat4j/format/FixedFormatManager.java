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

/**
 * Interface used to interact with fixed format annotations.
 * <p>
 * A <code>FixedFormatManager</code> is associated with one type of fixed format data.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.0.0
 */
public interface FixedFormatManager {

  /**
   * Create an instance of the fixedFormatClass and load the data string into the object according to the annotations.
   *
   * @param clazz the class to instantiate
   * @param data  the data to load
   * @param <T>   the type of the record class
   * @return an object loaded with the fixedformat data
   * @throws ParseException       in case that some specific parsing fails. Ex. a field couldn't be parsed according to some annotation instructions.
   *                              This exception contains detailed information telling what failed to be loaded.
   * @throws FixedFormatException in case the fixedFormatRecord class cannot be loaded. Ex. the Class wasn't annotated with a {@code @Record} annotation
   */
  <T> T load(Class<T> clazz, String data) throws FixedFormatException;

  /**
   * Exports {@code instance} into a fixed-width string representation.
   * The instance must be annotated with {@code @Record} and have {@code @Field} annotations on
   * the getters that are to be exported.
   *
   * @param instance the object to export
   * @param <T>      the type of the instance to export
   * @return a fixed-width string built from all {@code @Field}-annotated getters of {@code instance}
   * @throws FixedFormatException in case the instance couldn't be exported
   */
  <T> String export(T instance) throws FixedFormatException;

  /**
   * Exports {@code instance} into a fixed-width string representation, merging it on top of
   * {@code template}.
   * <p>
   * This is useful when most of a record's content is static: supply the static content as
   * {@code template} and only the dynamic fields will be overwritten.
   *
   * @param template the base string to merge the exported instance into
   * @param instance the object to export
   * @param <T>      the type of the instance to export
   * @return a fixed-width string with all {@code @Field}-annotated values merged into {@code template}
   * @throws FixedFormatException in case the instance couldn't be exported
   */
  <T> String export(String template, T instance) throws FixedFormatException;


}
