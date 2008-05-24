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

/**
 * Contains metadata for the fixed formatter.
 * Metadata is what kind of formatter to use and what kind of datatype.
 *
 * @author Jacob von Eyben www.ancientprogramming.com
 * @since 1.0.0
 */
public class FixedFormatMetadata {

  private int offset;
  private Class dataType;
  private Class<? extends FixedFormatter> formatter;

  public FixedFormatMetadata(int offset, Class dataType, Class<? extends FixedFormatter> formatter) {
    this.offset = offset;
    this.dataType = dataType;
    this.formatter = formatter;
  }

  public int getOffset() {
    return offset;
  }

  public Class getDataType() {
    return dataType;
  }

  public Class<? extends FixedFormatter> getFormatter() {
    return formatter;
  }


  public String toString() {
    return "FixedFormatMetadata{" +
        "offset=" + offset +
        ", dataType=" + dataType +
        ", formatter=" + formatter +
        '}';
  }
}
