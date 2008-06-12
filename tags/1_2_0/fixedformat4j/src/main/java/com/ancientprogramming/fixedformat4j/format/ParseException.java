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

import java.lang.reflect.Method;

/**
 * Used in cases where data couldn't be parse according to the context and instructions.
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.2.0
 */
public class ParseException extends FixedFormatException {

  public ParseException(String completeString, String data, Class clazz, Method method, FormatContext formatContext, FormatInstructions formatInstructions, Throwable e) {
    super("failed to parse '" + data + "' at offset " + formatContext.getOffset() + " as " + formatContext.getDataType().getName() + " from '" + completeString + "'. Got format instructions from " + clazz.getName() + "." + method.getName() + ". See details{" + formatContext.toString() + ", " +formatInstructions.toString() + "}", e);
  }
}
