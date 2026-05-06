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
package com.ancientprogramming.fixedformat4j.format.impl;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;

/**
 * Thread-local cache for {@link DecimalFormat} instances keyed by decimal-place count.
 * {@link DecimalFormat} is not thread-safe, so one instance per thread per precision is kept.
 * The locale-specific separator characters are captured once at construction time to avoid
 * per-call allocation from {@link DecimalFormatSymbols#getDecimalSeparator()} /
 * {@link DecimalFormatSymbols#getGroupingSeparator()}.
 */
class DecimalFormatCache {

  static final class State {
    final DecimalFormat format;
    final char decimalSeparator;
    final char groupingSeparator;
    final String zeroString;

    State(int decimals) {
      format = new DecimalFormat();
      format.setDecimalSeparatorAlwaysShown(true);
      format.setMaximumFractionDigits(decimals);
      DecimalFormatSymbols symbols = format.getDecimalFormatSymbols();
      decimalSeparator = symbols.getDecimalSeparator();
      groupingSeparator = symbols.getGroupingSeparator();
      zeroString = "0" + decimalSeparator + "0";
    }
  }

  private static final ThreadLocal<Map<Integer, State>> CACHE =
      ThreadLocal.withInitial(HashMap::new);

  private DecimalFormatCache() {}

  static State get(int decimals) {
    return CACHE.get().computeIfAbsent(decimals, State::new);
  }
}
