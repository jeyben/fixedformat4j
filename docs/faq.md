---
title: FAQ
---

# Frequently Asked Questions

## Can fixedformat4j help me parse large text files?

As the API stands it does not provide file-iteration utilities, but it does not constrain you in any way either.

Fixedformat4j concentrates on mapping one single line to a Java object. You are free to loop through any large text file and use the `FixedFormatManager` to create instances of Java objects for each line.

## Can I apply my own custom formatter?

Yes.

Extend `AbstractFixedFormatter<T>` (recommended) or implement `FixedFormatter<T>` directly. The two methods you must provide are `asObject` (string → your type) and `asString` (your type → string):

```java
public class CustomFormatter extends AbstractFixedFormatter<MyType> {

  @Override
  public MyType asObject(String value, FormatInstructions instructions) {
    // 'value' has already had padding stripped by the base class
    return MyType.parse(value);
  }

  @Override
  public String asString(MyType value, FormatInstructions instructions) {
    return value.toFixedString();
  }
}
```

To access supplementary annotation data (e.g., a pattern supplied via `@FixedFormatPattern`), use the `FormatInstructions` argument:

```java
String pattern = instructions.getFixedFormatPatternData().getPattern();
```

To instruct the manager to use your custom formatter, set the `formatter` attribute on the `@Field` annotation:

```java
@Field(offset = 1, length = 10, formatter = CustomFormatter.class)
```

See [Example 5 — Custom formatter](examples#example-5--custom-formatter) for a complete working implementation.

## Can I change how padding is stripped on read?

Yes. The default behaviour — strip `paddingChar` from the alignment side — is defined in `AbstractFixedFormatter.stripPadding`. Override it in a formatter subclass when you need different semantics, then wire it in with `@Field(formatter = ...)`.

**Preserve the raw value (no stripping):**

```java
public class RawStringFormatter extends StringFormatter {
  @Override
  protected String stripPadding(String value, FormatInstructions instructions) {
    return value;
  }
}
```

Use when leading or trailing characters are meaningful — e.g. a formatted identifier where the padding is part of the value. Round-trip export is preserved because no information is dropped.

**Strip padding from both ends:**

```java
public class BothSidesStringFormatter extends StringFormatter {
  @Override
  protected String stripPadding(String value, FormatInstructions instructions) {
    char pad = instructions.getPaddingChar();
    return Align.LEFT.remove(Align.RIGHT.remove(value, pad), pad);
  }
}
```

Use when a producer pads inconsistently. Note: export re-pads on the alignment side only, so round-trip is **not** preserved — opposite-side padding is permanently dropped on read.

**Strip all whitespace (including embedded):**

```java
public class AllWhitespaceStringFormatter extends StringFormatter {
  @Override
  protected String stripPadding(String value, FormatInstructions instructions) {
    return value.replaceAll("\\s+", "");
  }
}
```

Use when the source field contains internal whitespace that should be discarded. Round-trip is lossy.

> **Scope note:** `stripPadding` is the extension point for `String`, `Character`, `Boolean`, and enum fields. Numeric formatters (`AbstractNumberFormatter`) and date/time formatters (`AbstractPatternFormatter`) override `parse` / `stripPadding` with their own sign-aware and pattern-aware logic, so a custom padding strategy for those types must extend the corresponding concrete formatter and override the relevant method directly.

## How do I parse blank fields as a default value instead of throwing?

Numeric formatters call `Integer.parseInt` / `Long.parseLong` / `BigDecimal`'s constructor on the stripped slice, so a blank numeric slice throws `NumberFormatException` by default. Two approaches cover the "blank = default" convention without a custom annotation attribute.

**For reference types (`Integer`, `Long`, `BigDecimal`, `Date`, etc.) — use a POJO field default plus `nullChar`:**

When `nullChar` activates on a blank slice, the manager sets the computed value to `null` and **skips the setter entirely**, so whatever the POJO initialised the field with is preserved.

```java
@Record(length = 20)
public class Invoice {
  private Integer repairHours = 0; // default when the slice is all nullChar

  @Field(offset = 1, length = 4, paddingChar = '0', nullChar = ' ', align = Align.RIGHT)
  public Integer getRepairHours() { return repairHours; }
  public void setRepairHours(Integer h) { this.repairHours = h; }
}
```

- Blank slice `"    "` → setter skipped → `repairHours` stays at `0`.
- Non-blank slice `"0042"` → setter invoked → `repairHours = 42`.

The key activation rule: `nullChar` must be explicitly set (anything other than the default `'\0'` sentinel). Since 1.7.2, setting `nullChar == paddingChar` also works and enables the "blank-is-null" convention — useful when you do not have a distinct sentinel character available (e.g. all-spaces dates, all-zeros numerics).

**For primitive types (`int`, `long`, `double`, etc.) — use a lenient formatter subclass:**

Primitives cannot be set to `null`, so `@Field(nullChar = ...)` is rejected at validation time. Override `asObject` on the built-in formatter and substitute your default when the input is empty:

```java
public class LenientIntegerFormatter extends IntegerFormatter {
  @Override
  public Integer asObject(String string, FormatInstructions fi) {
    return string.isEmpty() ? 0 : super.asObject(string, fi);
  }
}

@Field(offset = 1, length = 4, paddingChar = '0', formatter = LenientIntegerFormatter.class)
public int getRepairHours() { return repairHours; }
```

The same pattern applies to `LongFormatter`, `ShortFormatter`, `DoubleFormatter`, `FloatFormatter`, and `BigDecimalFormatter`.

## How do I handle records with different layouts in the same file?

Define a separate `@Record`-annotated class for each layout. When reading the file line by line, inspect a discriminator field (such as a record-type code in a known column) and call `manager.load(...)` with the matching class:

```java
while ((line = reader.readLine()) != null) {
    String type = line.substring(0, 1); // type code in column 1
    if ("H".equals(type)) {
        headers.add(manager.load(HeaderRecord.class, line));
    } else if ("D".equals(type)) {
        details.add(manager.load(DetailRecord.class, line));
    }
}
```

See [Example 4 — Processing a file line by line](examples#example-4--processing-a-file-line-by-line) for a full file-reading snippet.

