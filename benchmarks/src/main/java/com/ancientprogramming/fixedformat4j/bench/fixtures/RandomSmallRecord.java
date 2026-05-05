package com.ancientprogramming.fixedformat4j.bench.fixtures;

import com.ancientprogramming.fixedformat4j.annotation.*;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

@Record
public class RandomSmallRecord {

    public static final String SAMPLE_DATA =
        "A" + "Y" + "BenchTest   " + "0042" + "00001000" +
        "000000012345" + "20240115" + "9876543210" +
        "000001500" + "00000000022500";

    private Character code;
    private Boolean flag;
    private String name;
    private Short count;
    private Integer id;
    private BigDecimal amount;
    private Date date;
    private Long serial;
    private Float ratio;
    private Double price;

    public static RandomSmallRecord sampleInstance() {
        RandomSmallRecord r = new RandomSmallRecord();
        r.setCode('A');
        r.setFlag(true);
        r.setName("BenchTest");
        r.setCount((short) 42);
        r.setId(1000);
        r.setAmount(new BigDecimal("123.45"));
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        r.setDate(cal.getTime());
        r.setSerial(9876543210L);
        r.setRatio(1.5f);
        r.setPrice(2.25);
        return r;
    }

    @Field(offset = 1, length = 1, align = Align.LEFT, paddingChar = ' ')
    public Character getCode() { return code; }
    public void setCode(Character v) { code = v; }

    @Field(offset = 2, length = 1)
    @FixedFormatBoolean(trueValue = "Y", falseValue = "N")
    public Boolean getFlag() { return flag; }
    public void setFlag(Boolean v) { flag = v; }

    @Field(offset = 3, length = 12, align = Align.LEFT, paddingChar = ' ')
    public String getName() { return name; }
    public void setName(String v) { name = v; }

    @Field(offset = 15, length = 4, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Short getCount() { return count; }
    public void setCount(Short v) { count = v; }

    @Field(offset = 19, length = 8, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Integer getId() { return id; }
    public void setId(Integer v) { id = v; }

    @Field(offset = 27, length = 12, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal v) { amount = v; }

    @Field(offset = 39, length = 8)
    @FixedFormatPattern("yyyyMMdd")
    public Date getDate() { return date; }
    public void setDate(Date v) { date = v; }

    @Field(offset = 47, length = 10, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Long getSerial() { return serial; }
    public void setSerial(Long v) { serial = v; }

    @Field(offset = 57, length = 9, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 3, useDecimalDelimiter = false)
    public Float getRatio() { return ratio; }
    public void setRatio(Float v) { ratio = v; }

    @Field(offset = 66, length = 14, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 4, useDecimalDelimiter = false)
    public Double getPrice() { return price; }
    public void setPrice(Double v) { price = v; }
}
