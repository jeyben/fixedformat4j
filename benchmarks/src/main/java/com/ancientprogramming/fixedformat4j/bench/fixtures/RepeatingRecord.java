package com.ancientprogramming.fixedformat4j.bench.fixtures;

import com.ancientprogramming.fixedformat4j.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Record
public class RepeatingRecord {

    public static final String SAMPLE_DATA =
        "BENCH     " + "00001000020000300004000050000600007000080000900010";

    private String name;
    private List<Integer> codes;

    public static RepeatingRecord sampleInstance() {
        RepeatingRecord r = new RepeatingRecord();
        r.setName("BENCH");
        List<Integer> codes = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            codes.add(i);
        }
        r.setCodes(codes);
        return r;
    }

    @Field(offset = 1, length = 10, align = Align.LEFT, paddingChar = ' ')
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Field(offset = 11, length = 5, count = 10, align = Align.RIGHT, paddingChar = '0')
    @FixedFormatNumber(sign = Sign.NOSIGN)
    public List<Integer> getCodes() {
        return codes;
    }

    public void setCodes(List<Integer> codes) {
        this.codes = codes;
    }
}
