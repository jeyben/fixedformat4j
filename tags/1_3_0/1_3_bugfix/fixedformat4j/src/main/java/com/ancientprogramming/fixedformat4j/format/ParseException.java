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
 * Used in cases where data couldn't be parse according to the {@link FormatContext} and {@link FormatInstructions}.
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.2.0
 */
public class ParseException extends FixedFormatException {

  private String completeText;
  private String failedText;
  private Class annotatedClass;
  private Method annotatedMethod;
  private FormatContext formatContext;
  private FormatInstructions formatInstructions;

  /**
   * Create an new instance
   * @param completeText the complete text that failed to be parsed
   * @param failedText the part of the complete text that failed the actual parsing according to the {@link #getFormatInstructions()}
   * @param annotatedClass the Class containing the fixedformat annotations
   * @param annotatedMethod the method containing the annotations that was used to trying to parse the text in {@link #getFailedText()}
   * @param formatContext the context within the parsing was tried
   * @param formatInstructions The format instructions used to try parsing the text in {@link #getFailedText()}
   * @param cause the reason why the data couldn't be parsed
   */
  public ParseException(String completeText, String failedText, Class annotatedClass, Method annotatedMethod, FormatContext formatContext, FormatInstructions formatInstructions, Throwable cause) {
    super("Failed to parse '" + failedText + "' at offset " + formatContext.getOffset() + " as " + formatContext.getDataType().getName() + " from '" + completeText + "'. Got format instructions from " + annotatedClass.getName() + "." + annotatedMethod.getName() + ". See details{" + formatContext.toString() + ", " +formatInstructions.toString() + "}", cause);
     this.completeText = completeText;
    this.failedText = failedText;
    this.annotatedClass = annotatedClass;
    this.annotatedMethod = annotatedMethod;
    this.formatContext = formatContext;
    this.formatInstructions = formatInstructions;
  }

  /**
   * Contains the complete text that failed to be parsed
   * @return String containing the complete text
   */
  public String getCompleteText() {
    return completeText;
  }

  /**
   * The part of the complete text that failed the actual parsing according to the {@link #getFormatInstructions()}
   * @return String containing the part that failed
   */
  public String getFailedText() {
    return failedText;
  }

  /**
   * The Class containing the fixedformat annotations
   * @return the Class
   */
  public Class getAnnotatedClass() {
    return annotatedClass;
  }

  /**
   * The method containing the annotations that was used to trying to parse the text in {@link #getFailedText()}
   * @return the annotated method
   */
  public Method getAnnotatedMethod() {
    return annotatedMethod;
  }

  /**
   * The context within the parsing was tried
   * @return the format context
   */
  public FormatContext getFormatContext() {
    return formatContext;
  }

  /**
   * The format instructions used to try parsing the text in {@link #getFailedText()}
   * @return the format instructions
   */
  public FormatInstructions getFormatInstructions() {
    return formatInstructions;
  }
}
