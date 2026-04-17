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
package com.ancientprogramming.fixedformat4j.format.data;

import com.ancientprogramming.fixedformat4j.annotation.EnumFormat;

/**
 * Immutable data object mirroring the {@code @FixedFormatEnum} annotation values.
 *
 * @author Jacob von Eyben - <a href="https://eybenconsult.com">https://eybenconsult.com</a>
 * @since 1.7.0
 */
public class FixedFormatEnumData {

  public static final FixedFormatEnumData DEFAULT = new FixedFormatEnumData(EnumFormat.LITERAL);

  private final EnumFormat enumFormat;

  /**
   * Creates an enum data holder with the given format mode.
   *
   * @param enumFormat the serialization format for the enum field
   */
  public FixedFormatEnumData(EnumFormat enumFormat) {
    this.enumFormat = enumFormat;
  }

  /**
   * Returns the serialization format.
   *
   * @return the {@link EnumFormat} for this field
   */
  public EnumFormat getEnumFormat() {
    return enumFormat;
  }

  @Override
  public String toString() {
    return String.format("FixedFormatEnumData{enumFormat=%s}", enumFormat);
  }
}
