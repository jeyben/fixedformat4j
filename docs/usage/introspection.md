---
title: Schema introspection
---

# Schema introspection

Since 1.9.0 the field layout of a `@Record` class can be queried at runtime through the
`FixedFormatIntrospector` interface, implemented by `FixedFormatManagerImpl`. This serves
tooling built on top of the annotated schema — documentation generators, UI form builders,
format negotiation (detecting offset/length drift between record class versions), and layout
assertions in tests — without re-implementing annotation scanning.

```java
FixedFormatIntrospector introspector = new FixedFormatManagerImpl();

for (FieldInfo field : introspector.introspect(CustomerRecord.class)) {
  System.out.printf("%-15s offset=%2d length=%2d type=%s%n",
      field.getPropertyName(), field.getOffset(), field.getLength(),
      field.getDataType().getSimpleName());
}
```

```
customerId      offset= 1 length=10 type=String
customerName    offset=11 length=20 type=String
```

`FixedFormatIntrospector` is a separate interface rather than an addition to
`FixedFormatManager`, so existing third-party manager implementations stay source and binary
compatible; callers that only need schema access can depend on the narrow contract.

## FieldInfo

`introspect()` returns one immutable `FieldInfo` per effective `@Field`, ordered by offset.
A getter carrying `@Fields` contributes one entry per inner `@Field`.

| Accessor | Meaning |
|---|---|
| `getPropertyName()` | Bean-style property name (`getCustomerId()` → `customerId`); for Java records, the component name as-is |
| `getOffset()` / `getLength()` | 1-based offset; length `-1` = rest-of-line |
| `getDataType()` | Declared Java type (the collection/array type for repeating fields) |
| `getEffectiveAlignment()` | Resolved `LEFT`/`RIGHT` — record-level defaults already applied, never `INHERIT` |
| `getPaddingChar()` | Padding character |
| `getNullChar()` / `getNullValue()` | Null sentinels (`UNSET_NULL_CHAR` / `""` when not configured) |
| `getFormatterClass()` | Configured formatter (`ByTypeFormatter` unless overridden) |
| `getRepeatCount()` | `count` attribute; `1` for non-repeating fields |
| `isNestedRecord()` | `true` when the field type is itself a `@Record` class |

## Validation preflight

`introspect()` triggers the same one-time metadata build and annotation validation as
`load()`/`export()`, so it doubles as a startup preflight check: an invalid configuration
(bad date pattern, enum too wide, misconfigured null sentinel, …) throws
`FixedFormatException` at introspection time. For build-time validation, see
[Compile-time validation](compile-time-validation).

Performance: the underlying scan and validation run **once per class** (the same cached
metadata used by `load()`/`export()`); each `introspect()` call performs only an
O(number of fields) mapping with no I/O and no reflection beyond the cached metadata.
