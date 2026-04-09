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

---

## Example 6 — Field annotations and Lombok

Since 1.5.0, `@Field` can be placed directly on a Java field instead of its getter. The manager derives the getter/setter by convention, so the two styles below are fully equivalent.

**Plain POJO** — annotations on fields, getters written explicitly:

```java
@Record
public class EmployeeRecord {

  @Field(offset = 1, length = 12)
  private String name;

  @Field(offset = 13, length = 5, align = Align.RIGHT, paddingChar = '0')
  private Integer employeeId;

  @Field(offset = 18, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  private LocalDate hireDate;

  @Field(offset = 26, length = 1)
  @FixedFormatBoolean(trueValue = "Y", falseValue = "N")
  private Boolean active;

  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public Integer getEmployeeId() { return employeeId; }
  public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }
  public LocalDate getHireDate() { return hireDate; }
  public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
  public Boolean getActive() { return active; }
  public void setActive(Boolean active) { this.active = active; }
}
```

**With Lombok** — same annotations on fields, getters/setters generated automatically:

```java
@Getter @Setter @NoArgsConstructor
@Record
public class EmployeeRecord {

  @Field(offset = 1, length = 12)
  private String name;

  @Field(offset = 13, length = 5, align = Align.RIGHT, paddingChar = '0')
  private Integer employeeId;

  @Field(offset = 18, length = 8)
  @FixedFormatPattern("yyyyMMdd")
  private LocalDate hireDate;

  @Field(offset = 26, length = 1)
  @FixedFormatBoolean(trueValue = "Y", falseValue = "N")
  private Boolean active;
}
```

Both classes load and export identically:

```java
FixedFormatManager manager = new FixedFormatManagerImpl();

String line = "Jane Doe    0004220260101Y";
EmployeeRecord emp = manager.load(EmployeeRecord.class, line);

System.out.println(emp.getName());       // "Jane Doe"
System.out.println(emp.getEmployeeId()); // 42
System.out.println(emp.getHireDate());   // 2026-01-01
System.out.println(emp.getActive());     // true

System.out.println(manager.export(emp));
// "Jane Doe    0004220260101Y"
```

**Conflict behaviour:** if `@Field` is present on both a field and its getter, an error is logged and the field annotation is used. It is recommended to annotate only one location.

## Example 7 — Repeating fields

Some fixed-width formats pack several consecutive slots of the same type into a single record — for example, a shipment record that lists up to four package weights in a row. Before 1.6.0 this required a separate `@Field` for each slot. With `count` you declare it once.

**Scenario:** each line in a freight file holds a shipment ID (6 chars) followed by up to four package weights in grams, each stored as a 6-character right-aligned zero-padded integer. Unused trailing slots are `"000000"`.

```
// positions: 1-6 = shipment ID, 7-12 = weight 1, 13-18 = weight 2, 19-24 = weight 3, 25-30 = weight 4
SHIP01002500010000000000000000
```

```java
@Record
public class ShipmentRecord {

  private String shipmentId;
  private List<Integer> packageWeights;

  @Field(offset = 1, length = 6)
  public String getShipmentId() { return shipmentId; }
  public void setShipmentId(String shipmentId) { this.shipmentId = shipmentId; }

  // Four consecutive 6-character integer slots starting at offset 7
  @Field(offset = 7, length = 6, count = 4, align = Align.RIGHT, paddingChar = '0')
  public List<Integer> getPackageWeights() { return packageWeights; }
  public void setPackageWeights(List<Integer> packageWeights) { this.packageWeights = packageWeights; }
}
```

```java
FixedFormatManager manager = new FixedFormatManagerImpl();

String line = "SHIP01002500010000000000000000";
ShipmentRecord record = manager.load(ShipmentRecord.class, line);

System.out.println(record.getShipmentId());      // "SHIP01"
System.out.println(record.getPackageWeights());  // [2500, 1000, 0, 0]

// Update the first two weights and export
record.setPackageWeights(List.of(3200, 1800, 500, 0));
System.out.println(manager.export(record));
// "SHIP01003200001800000500000000"
```

**Using an array instead of a List:**

```java
@Field(offset = 7, length = 6, count = 4, align = Align.RIGHT, paddingChar = '0')
public Integer[] getPackageWeights() { return packageWeights; }
```

**Lenient export — partial collection:**

If the collection may have fewer elements than `count` (e.g. a shipment with only 2 packages), set `strictCount = false`. The remaining slots are left as the template value or untouched:

```java
@Field(offset = 7, length = 6, count = 4, align = Align.RIGHT, paddingChar = '0',
       strictCount = false)
public List<Integer> getPackageWeights() { return packageWeights; }
```

```java
record.setPackageWeights(List.of(3200, 1800)); // only 2 of 4 slots provided
System.out.println(manager.export(record));
// "SHIP01003200001800[slots 3 and 4 unchanged from original record]"
// Warning is logged: collection size (2) < count (4)
```

With `strictCount = true` (the default), passing a list of the wrong size throws a `FixedFormatException` at export time.

