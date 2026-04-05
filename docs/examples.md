---
title: Examples
---

# Examples

Each example below is self-contained and demonstrates a specific feature. All examples use the same entry point:

```java
FixedFormatManager manager = new FixedFormatManagerImpl();
```

---

## Example 1 — Financial record with decimals and signed amounts

This example shows `@FixedFormatDecimal` for implicit-decimal storage and `@FixedFormatNumber` for signed values — common requirements in financial and mainframe file formats.

With `useDecimalDelimiter = false`, the decimal point is **not stored** in the string; instead the last `decimals` digits are treated as the fractional part. This saves a character per field compared to storing `"123.50"`.

```java
@Record
public class TransactionRecord {

  private BigDecimal amount;
  private BigDecimal balance;

  // "0001250" → 12.50  (last 2 digits are decimals, no delimiter stored)
  @Field(offset = 1, length = 7, align = Align.RIGHT, paddingChar = '0')
  @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }

  // "-000500" → -5.00  (sign prepended, takes up one of the 7 characters)
  @Field(offset = 8, length = 7, align = Align.RIGHT, paddingChar = '0')
  @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
  @FixedFormatNumber(sign = Sign.PREPEND)
  public BigDecimal getBalance() { return balance; }
  public void setBalance(BigDecimal balance) { this.balance = balance; }
}
```

```java
String line = "0001250-000500";
TransactionRecord record = manager.load(TransactionRecord.class, line);

System.out.println(record.getAmount());   // 12.50
System.out.println(record.getBalance());  // -5.00

record.setAmount(new BigDecimal("99.99"));
System.out.println(manager.export(record));
// "0009999-000500"
```

**Decimal annotation options at a glance:**

| `useDecimalDelimiter` | String stored | Parsed value |
|-----------------------|--------------|-------------|
| `false` (default) | `"001250"` | `12.50` |
| `true` | `"012.50"` | `12.50` |

---

## Example 2 — Boolean flags with custom true/false values

The default boolean representation is `"T"` / `"F"`. Use `@FixedFormatBoolean` to override this — for example to match a legacy format that uses `"Y"` / `"N"` or `"1"` / `"0"`.

```java
@Record
public class StatusRecord {

  private Boolean active;
  private Boolean verified;

  @Field(offset = 1, length = 1)
  @FixedFormatBoolean(trueValue = "Y", falseValue = "N")
  public Boolean getActive() { return active; }
  public void setActive(Boolean active) { this.active = active; }

  @Field(offset = 2, length = 1)
  @FixedFormatBoolean(trueValue = "1", falseValue = "0")
  public Boolean getVerified() { return verified; }
  public void setVerified(Boolean verified) { this.verified = verified; }
}
```

```java
String line = "Y1";
StatusRecord record = manager.load(StatusRecord.class, line);

System.out.println(record.getActive());    // true
System.out.println(record.getVerified()); // true

record.setActive(false);
System.out.println(manager.export(record));
// "N1"
```

---

## Example 3 — Right-aligned, zero-padded integers

Fixed-width formats often store numbers right-aligned and zero-padded so that every record has the same layout. The `align` and `paddingChar` attributes on `@Field` control this.

```java
@Record
public class OrderRecord {

  private String productCode;
  private Integer quantity;
  private Integer unitPriceCents;

  // Left-aligned text, padded with spaces (the default)
  @Field(offset = 1, length = 10)
  public String getProductCode() { return productCode; }
  public void setProductCode(String productCode) { this.productCode = productCode; }

  // Right-aligned number, padded with zeros on the left
  @Field(offset = 11, length = 5, align = Align.RIGHT, paddingChar = '0')
  public Integer getQuantity() { return quantity; }
  public void setQuantity(Integer quantity) { this.quantity = quantity; }

  // Right-aligned, zero-padded price in cents
  @Field(offset = 16, length = 8, align = Align.RIGHT, paddingChar = '0')
  public Integer getUnitPriceCents() { return unitPriceCents; }
  public void setUnitPriceCents(Integer unitPriceCents) { this.unitPriceCents = unitPriceCents; }
}
```

```java
String line = "WIDGET-A  000120000150";
OrderRecord record = manager.load(OrderRecord.class, line);

System.out.println(record.getProductCode());    // "WIDGET-A"
System.out.println(record.getQuantity());       // 12
System.out.println(record.getUnitPriceCents()); // 150

record.setQuantity(5);
System.out.println(manager.export(record));
// "WIDGET-A  000050000150"
```

**Alignment at a glance:**

| `align` | Padding side | Trim side | Typical use |
|---------|-------------|-----------|------------|
| `Align.LEFT` (default) | Right | Right | Text fields |
| `Align.RIGHT` | Left | Left | Numeric fields |

---

## Example 4 — Processing a file line by line

fixedformat4j maps a **single line** to a Java object. To process a whole file, loop through the lines yourself and call `manager.load(...)` for each one.

```java
FixedFormatManager manager = new FixedFormatManagerImpl();
List<EmployeeRecord> employees = new ArrayList<>();

try (BufferedReader reader = new BufferedReader(new FileReader("employees.txt"))) {
    String line;
    while ((line = reader.readLine()) != null) {
        if (!line.isBlank()) {
            employees.add(manager.load(EmployeeRecord.class, line));
        }
    }
}

// Process the loaded records
for (EmployeeRecord emp : employees) {
    System.out.println(emp.getName() + " — ID: " + emp.getEmployeeId());
}
```

If a single file contains records of more than one type, read a discriminator field first and branch to the appropriate class:

```java
while ((line = reader.readLine()) != null) {
    String recordType = line.substring(0, 1); // type code in column 1
    if ("E".equals(recordType)) {
        employees.add(manager.load(EmployeeRecord.class, line));
    } else if ("M".equals(recordType)) {
        managers.add(manager.load(ManagerRecord.class, line));
    }
}
```

---

## Example 5 — Custom formatter

When no built-in formatter fits your field type, implement `FixedFormatter<T>` directly.

This example maps a field to `java.time.YearMonth` (e.g. `"2024-03"` for March 2024):

```java
public class YearMonthFormatter extends AbstractFixedFormatter<YearMonth> {

  @Override
  public YearMonth asObject(String value, FormatInstructions instructions) {
    // 'value' arrives already stripped of padding by the base class
    return YearMonth.parse(value, DateTimeFormatter.ofPattern("yyyy-MM"));
  }

  @Override
  public String asString(YearMonth value, FormatInstructions instructions) {
    return value.format(DateTimeFormatter.ofPattern("yyyy-MM"));
  }
}
```

Wire it in with the `formatter` attribute on `@Field`:

```java
@Record
public class ReportRecord {

  private YearMonth reportPeriod;

  @Field(offset = 1, length = 7, formatter = YearMonthFormatter.class)
  public YearMonth getReportPeriod() { return reportPeriod; }
  public void setReportPeriod(YearMonth reportPeriod) { this.reportPeriod = reportPeriod; }
}
```

```java
String line = "2024-03";
ReportRecord record = manager.load(ReportRecord.class, line);

System.out.println(record.getReportPeriod()); // 2024-03

record.setReportPeriod(YearMonth.of(2025, 1));
System.out.println(manager.export(record));
// "2025-01"
```

For access to supplementary annotation data inside the formatter (e.g., `@FixedFormatPattern`), use the `FormatInstructions` argument:

```java
String pattern = instructions.getFixedFormatPatternData().getPattern();
```

See the [FAQ](faq#can-i-apply-my-own-custom-formatter) for more details on the `FixedFormatter` interface.

---

[Home](index) | [Quick Start](quickstart) | [Usage](usage/) | [Get It](get-it) | [FAQ](faq)
