package com.ancientprogramming.fixedformat4j.bench.fixtures;

import com.ancientprogramming.fixedformat4j.annotation.*;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;

@Record
public class SmallRecord {

    public static final String SAMPLE_DATA = "John Smith          0000000042000000000123456202401151";

    private String name;
    private Integer id;
    private BigDecimal amount;
    private Date date;
    private Boolean active;

    public static SmallRecord sampleInstance() {
        SmallRecord r = new SmallRecord();
        r.setName("John Smith");
        r.setId(42);
        r.setAmount(new BigDecimal("1234.56"));
        Calendar cal = Calendar.getInstance();
        cal.set(2024, Calendar.JANUARY, 15, 0, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        r.setDate(cal.getTime());
        r.setActive(true);
        return r;
    }

    @Field(offset = 1, length = 20, align = Align.LEFT, paddingChar = ' ')
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Field(offset = 21, length = 10, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Field(offset = 31, length = 15, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatDecimal(decimals = 2, useDecimalDelimiter = false)
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Field(offset = 46, length = 8)
    @FixedFormatPattern("yyyyMMdd")
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Field(offset = 54, length = 1)
    @FixedFormatBoolean(trueValue = "1", falseValue = "0")
    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
