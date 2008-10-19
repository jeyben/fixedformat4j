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
 * Contains context for loading and exporting fixedformat data.
 * The context describes what kind of formatter to use, what datatype to convert and what offset to fetch data from.
 *
 * @author Jacob von Eyben - http://www.ancientprogramming.com
 * @since 1.0.0
 */
public class FormatContext<T> {

  private int offset;
  private Class<T> dataType;
  private Class<? extends FixedFormatter<T>> formatter;

  public FormatContext(int offset, Class<T> dataType, Class<? extends FixedFormatter<T>> formatter) {
    this.offset = offset;
    this.dataType = dataType;
    this.formatter = formatter;
  }

  public int getOffset() {
    return offset;
  }

  public Class<T> getDataType() {
    return dataType;
  }

  public Class<? extends FixedFormatter<T>> getFormatter() {
    return formatter;
  }


  public String toString() {
    return "FormatContext{" +
        "offset=" + offset +
        ", dataType=" + dataType.getName() +
        ", formatter=" + formatter.getName() +
        '}';
  }
}
