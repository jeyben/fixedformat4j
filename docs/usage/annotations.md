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
| `align` | `Align` | no | `Align.LEFT` | Default alignment applied to all fields in the record. Individual `@Field` annotations may override it with an explicit `align` value. |

## @Field

Used on getter methods or directly on fields. Contains basic mapping instructions. Required for getter/setter pairs that should be mapped to and from string representation.

When placed on a field, the manager derives the getter and setter by name convention: a field named `foo` expects `getFoo()` / `setFoo()`, or `isFoo()` / `setFoo()` for boolean types. This works with both explicit getters and Lombok-generated ones. If `@Field` is present on both the field and its getter, an error is logged and the field annotation takes precedence.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `offset` | `int` | yes | — | The 1-based offset in the string where the data for this field starts. |
| `length` | `int` | yes | — | The length for this field in string representation. |
| `align` | `Align` | no | `Align.INHERIT` | How to align the field value when represented as a string. `Align.INHERIT` defers to the `@Record`-level default; falls back to `Align.LEFT` when no record-level default is set. |
| `paddingChar` | `char` | no | `' '` | The character to pad with when the length is longer than the field value. |
| `formatter` | `Class<FixedFormatter>` | no | `ByTypeFormatter.class` | The formatter to use when reading and writing the field. |
| `count` | `int` | no | `1` | Number of consecutive repetitions of this field. When greater than 1, the getter/setter must use an array or an ordered `Collection` (`List`, `Set`, `SortedSet`, etc.). Each repetition occupies `length` characters, starting at `offset + length * index`. |
| `strictCount` | `boolean` | no | `true` | Only relevant when `count > 1`. If `true` (default), a size mismatch between the array/collection and `count` during export throws a `FixedFormatException`. If `false`, a warning is logged and export proceeds with `min(count, actualSize)` elements. |
| `nullChar` | `char` | no | `'\0'` | Sentinel character that represents a null value. Null-aware handling is enabled only when `nullChar` differs from `paddingChar`. On load: if every character in the field slice equals `nullChar`, the setter is not invoked and the field remains `null`. On export: if the getter returns `null`, the field is emitted as `nullChar` repeated `length` times, bypassing the formatter. Not applicable when `count > 1`. |

**Alignment values:**

| Value | Padding side | Trim side | Typical use |
|-------|-------------|-----------|------------|
| `Align.LEFT` | Right | Right | Text fields — value starts at the left, spaces fill the right |
| `Align.RIGHT` | Left | Left | Numeric fields — value ends at the right, padding fills the left |
| `Align.INHERIT` (default) | — | — | Inherits the `@Record`-level `align` setting; falls back to `Align.LEFT` when no record-level default is set |

Example: a 5-character field with value `"Hi"` is stored as `"Hi   "` with `LEFT` and `"   Hi"` with `RIGHT`.

### Repeating fields

When a fixed-format record contains multiple consecutive slots of the same type, use `count` instead of listing individual `@Field` annotations manually:

```java
// Three 3-character product codes at positions 1–3, 4–6, 7–9
@Field(offset = 1, length = 3, count = 3)
public String[] getProductCodes() { return productCodes; }

// Same with a List
@Field(offset = 1, length = 3, count = 3)
public List<String> getProductCodes() { return productCodes; }

// Lenient export: log a warning instead of throwing when sizes differ
@Field(offset = 1, length = 3, count = 3, strictCount = false)
public String[] getProductCodes() { return productCodes; }
```

Supported return types for `count > 1`: `T[]` (array), `List`, `LinkedList`, `Set` (loaded as `LinkedHashSet`), `SortedSet` (loaded as `TreeSet`), `Collection`.

### Nullable fields

By default a fixed-width field has no concept of null — an all-spaces field loads as an empty string or zero, not `null`. The `nullChar` attribute opts a single field into null-aware handling by designating a sentinel character.

**Activation rule:** null-aware handling is enabled only when `nullChar` differs from `paddingChar`. The built-in default (`'\0'`) can never appear in a real fixed-width payload, so all existing fields retain their pre-1.7.1 behaviour unless you explicitly set `nullChar`.

```java
// "     " (five spaces) → null   "00042" → 42
@Field(offset = 1, length = 5, align = Align.RIGHT, paddingChar = '0', nullChar = ' ')
public Integer getAmount() { return amount; }
public void setAmount(Integer amount) { this.amount = amount; }
```

- **On load** — if every character in the field slice equals `nullChar`, the setter is not invoked and the field stays `null` (primitive fields keep their JVM default).
- **On export** — if the getter returns `null`, the field is emitted as `nullChar` × `length`, bypassing the formatter entirely.

`nullChar` is not supported for repeating fields (`count > 1`).

## @Fields

Used on getter methods or directly on fields when a single property maps to **multiple
non-uniform positions** in the same record — for example, a date field stored at two
different offsets in two different formats.

> **Note:** `@Fields` is only needed when the field positions or formats differ between
> entries. For consecutive slots of the **same** length and format (e.g. three 5-character
> codes in a row), use `@Field(count = N)` on a single getter instead — it is simpler and
> less error-prone.

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

| Field type | Formatter class | Pattern syntax | Default pattern |
|---|---|---|---|
| `java.util.Date` | `DateFormatter` | `SimpleDateFormat` patterns | `yyyyMMdd` |
| `java.time.LocalDate` | `LocalDateFormatter` | `DateTimeFormatter` patterns | `yyyy-MM-dd` |
| `java.time.LocalDateTime` | `LocalDateTimeFormatter` | `DateTimeFormatter` patterns | `yyyy-MM-dd'T'HH:mm:ss` |

All three types share the same `@FixedFormatPattern` annotation. The annotation is **optional** — omit it to use the default pattern shown above, and only add it when your format differs.

**Example with `LocalDate`:**

```java
// Default pattern (yyyy-MM-dd) — no annotation needed
@Field(offset = 1, length = 10)
public LocalDate getEventDate() { return eventDate; }

// Custom pattern
@Field(offset = 1, length = 8)
@FixedFormatPattern("yyyyMMdd")
public LocalDate getEventDate() { return eventDate; }
```

String `"2026-04-05"` parses to `LocalDate.of(2026, 4, 5)` with the default pattern.

**Example with `LocalDateTime`:**

```java
// Default pattern (yyyy-MM-dd'T'HH:mm:ss) — no annotation needed
@Field(offset = 1, length = 19)
public LocalDateTime getCreatedAt() { return createdAt; }

// Custom pattern
@Field(offset = 1, length = 14)
@FixedFormatPattern("yyyyMMddHHmmss")
public LocalDateTime getCreatedAt() { return createdAt; }
```

String `"2026-04-09T14:30:00"` parses to `LocalDateTime.of(2026, 4, 9, 14, 30, 0)` with the default pattern.

**Example with `Date`:**

```java
// Default pattern (yyyyMMdd) — no annotation needed
@Field(offset = 1, length = 8)
public Date getHireDate() { return hireDate; }

// Custom pattern
@Field(offset = 1, length = 10)
@FixedFormatPattern("yyyy-MM-dd")
public Date getHireDate() { return hireDate; }
```

