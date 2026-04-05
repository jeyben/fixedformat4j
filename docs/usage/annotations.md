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

## @FixedFormatNumber

Configures how numeric fields are signed in their string representation.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `sign` | `Sign` | no | `Sign.NOSIGN` | Whether and how to sign the number. Options: `NOSIGN`, prepend, or append a sign character. |
| `positiveSign` | `char` | no | `'+'` | Character representing a positive sign. |
| `negativeSign` | `char` | no | `'-'` | Character representing a negative sign. |

## @FixedFormatDecimal

Configures how decimal numbers are handled.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `decimals` | `int` | no | `2` | Number of decimal places the field contains. |
| `useDecimalDelimiter` | `boolean` | no | `false` | Whether the field includes a decimal delimiter in its string representation. |
| `decimalDelimiter` | `char` | no | `'.'` | The decimal delimiter character to use when `useDecimalDelimiter` is `true`. |

## @FixedFormatPattern

Configures pattern-based formatting. Currently used for `Date` fields via `SimpleDateFormat` patterns.

| Attribute | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `value` | `String` | yes | — | The pattern string, e.g. `"yyyy-MM-dd"` for dates. Uses the same syntax as `SimpleDateFormat`. |

---

[Home](../index) | [Usage](index) | [Nested Records](nested-records)
