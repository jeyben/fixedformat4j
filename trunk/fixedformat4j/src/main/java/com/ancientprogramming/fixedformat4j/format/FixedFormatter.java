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

import com.ancientprogramming.fixedformat4j.annotation.Field;
import com.ancientprogramming.fixedformat4j.exception.FixedFormatException;
import com.ancientprogramming.fixedformat4j.format.impl.DateFormatter;

/**
 * Formatter capable of transforming data to and from a string used in text records.
 * <p/>
 * A concrete class is used together with the @{@link Field} annotation.
 * <p/>
 * Example: <p><code>@Field(offset = 1, length = 20, formatter = {@link DateFormatter}.class)</code></p>
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public interface FixedFormatter<T> {

  /**
   * Parses the given String value according to the format instruction.
   * How the data is parsed and to what object is defined by the implementor.
   * @param value the data to parse.
   * @param instructions contains the instructions telling how to parse the value
   * @return An instance of T after the value was parsed according to the instructions
   * @throws FixedFormatException if the value could not be parsed according to the instructions
   */
  T parse(String value, FormatInstructions instructions) throws FixedFormatException;


  /**
   * Formats an instance of T according to the instructions
   * @param value the object to format
   * @param instructions contains the instructions telling how to format the value
   * @return a string representation of the value object after it was formatted according to the instructions
   * @throws FixedFormatException if the value could not be formatted according to the instructions
   */
  String format(T value, FormatInstructions instructions) throws FixedFormatException;
}
