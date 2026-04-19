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
package com.ancientprogramming.fixedformat4j.exception;

import java.io.IOException;

/**
 * Thrown when an {@link java.io.IOException} occurs while reading a fixed-format file.
 * Wraps the original IOException so callers can catch IO failures distinctly from parse
 * failures in stream pipelines.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.8.0
 */
public class FixedFormatIOException extends FixedFormatException {

  /**
   * Creates a new exception with the given detail message and IO cause.
   *
   * @param message a human-readable description of the context in which the IO error occurred
   * @param cause   the underlying {@link IOException}
   */
  public FixedFormatIOException(String message, IOException cause) {
    super(message, cause);
  }
}
