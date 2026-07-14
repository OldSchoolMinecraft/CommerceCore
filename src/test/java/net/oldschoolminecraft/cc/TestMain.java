package net.oldschoolminecraft.cc;

import static org.apache.poi.ss.formula.functions.Finance.*;

public class TestMain
{
    public static void main(String[] args)
    {
        double value = pmt((0.0975/360*366/12),
                3*12,
                3195,
                0,
                0);
        // pmt gives us a negative number so make it positive
        value = Math.abs(-value);

        // round to two decimal places
        value = Math.round(value * 100);
        value = value/100;

        // spit out the final value
        System.out.println(value);
    }
}
