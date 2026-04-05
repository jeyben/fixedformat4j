---
title: Annotations
---

# Annotations

Annotations instruct the manager on how to handle properties when loading and exporting classes to and from strings.

## @Record

Used on classes. Marks that the class contains `@Field` annotations on its getter methods and can be imported and exported to and from string representation using the `FixedFormatManager`.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `length` | `int` | no | `-1` | Minimum length the string representation should have. If the string is shorter it is padded with `paddingChar`. |
| `paddingChar` | `char` | no | `' '` | The character to use when padding is needed. |

## @Field

Used on getter methods. Contains basic mapping instructions. Required for getter/setter pairs that should be mapped to and from string representation.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `offset` | `int` | yes | — | The 1-based offset in the string where the data for this field starts. |
| `length` | `int` | yes | — | The length for this field in string representation. |
| `align` | `Align` | no | `Align.LEFT` | How to align the field value when represented as a string. |
| `paddingChar` | `char` | no | `' '` | The character to pad with when the length is longer than the field value. |
| `formatter` | `Class<FixedFormatter>` | no | `ByTypeFormatter.class` | The formatter to use when reading and writing the field. |

**Alignment values:**

| Value | Padding side | Trim side | Typical use |
|-------|-------------|-----------|------------|
| `Align.LEFT` (default) | Right | Right | Text fields — value starts at the left, spaces fill the right |
| `Align.RIGHT` | Left | Left | Numeric fields — value ends at the right, padding fills the left |

Example: a 5-character field with value `"Hi"` is stored as `"Hi   "` with `LEFT` and `"   Hi"` with `RIGHT`.

## @Fields

Used on getter methods when a single field maps to more than one position in a string.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `value` | `Field[]` | yes | — | Array of `@Field` annotations. When reading, only the first `@Field` in the array is used. |

## @FixedFormatBoolean

Optional annotation on getter methods when the field type is `Boolean`. Configures the string representation of `true` and `false`.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `trueValue` | `String` | no | `"T"` | String representation when the field equals `Boolean.TRUE`. |
| `falseValue` | `String` | no | `"F"` | String representation when the field equals `Boolean.FALSE`. |

**Example:** to use `"Y"` / `"N"` instead of the defaults:

```java
@Field(offset = 1, length = 1)
@FixedFormatBoolean(trueValue = "Y", falseValue = "N")
public Boolean getActive() { return active; }
```

String `"Y"` parses to `true`; `manager.export(...)` writes `"N"` when the value is `false`.

## @FixedFormatNumber

Configures how numeric fields are signed in their string representation.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `sign` | `Sign` | no | `Sign.NOSIGN` | Whether and how to sign the number. |
| `positiveSign` | `char` | no | `'+'` | Character representing a positive sign. |
| `negativeSign` | `char` | no | `'-'` | Character representing a negative sign. |

**Sign enum values:**

| Value | Behaviour | Example (value −123, length 5) |
|-------|-----------|-------------------------------|
| `Sign.NOSIGN` (default) | No sign character; alignment handles padding | `"  123"` (negative values stored as positive) |
| `Sign.PREPEND` | Sign character placed before the digits | `"-0123"` |
| `Sign.APPEND` | Sign character placed after the digits | `"0123-"` |

**Example:**

```java
@Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0')
@FixedFormatNumber(sign = Sign.PREPEND)
public Integer getBalance() { return balance; }
```

Value `−123` is stored as `"-0123"`; value `123` as `"+0123"` (using the default `positiveSign`).

## @FixedFormatDecimal

Configures how decimal numbers are handled.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `decimals` | `int` | no | `2` | Number of decimal places the field contains. |
| `useDecimalDelimiter` | `boolean` | no | `false` | Whether the field includes a decimal delimiter in its string representation. |
| `decimalDelimiter` | `char` | no | `'.'` | The decimal delimiter character to use when `useDecimalDelimiter` is `true`. |
| `roundingMode` | `int` | no | `BigDecimal.ROUND_HALF_UP` | Rounding mode used when the value has more decimal places than `decimals` allows. |

When `useDecimalDelimiter` is `false` (the default), the decimal point is **implicit**: the last `decimals` digits are treated as the fractional part. This is common in legacy and mainframe formats where every character counts.

**Example:**

```java
@Field(offset = 1, length = 6, align = Align.RIGHT, paddingChar = '0')
@FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
public BigDecimal getAmount() { return amount; }
```

| String stored | Parsed value |
|--------------|-------------|
| `"001250"` | `12.50` |
| `"000099"` | `0.99` |

With `useDecimalDelimiter = true` and length 7: `"012.50"` parses to `12.50`.

## @FixedFormatPattern

Configures pattern-based formatting for date fields.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `value` | `String` | yes | `"yyyyMMdd"` | The pattern string, e.g. `"yyyyMMdd"` or `"yyyy-MM-dd"`. |

Supported field types and their underlying formatter:

| Field type | Formatter class | Pattern syntax |
|---|---|---|
| `java.util.Date` | `DateFormatter` | `SimpleDateFormat` patterns |
| `java.time.LocalDate` | `LocalDateFormatter` | `DateTimeFormatter` patterns |

Both types use the same pattern syntax (e.g. `yyyyMMdd`, `yyyy-MM-dd`, `ddMMyyyy`).

**Example with `LocalDate`:**

```java
@Field(offset = 1, length = 8)
@FixedFormatPattern("yyyyMMdd")
public LocalDate getEventDate() { return eventDate; }
```

String `"20260405"` parses to `LocalDate.of(2026, 4, 5)`; exporting writes `"20260405"` back.

**Example with `Date`:**

```java
@Field(offset = 1, length = 8)
@FixedFormatPattern("yyyyMMdd")
public Date getHireDate() { return hireDate; }
```

---

[Home](../index) | [Usage](index) | [Nested Records](nested-records) | [Examples](../examples)
