package net.oldschoolminecraft.cc.util;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationValue
{
    private final long amount;
    private final TemporalUnit unit;

    public DurationValue(long amount, TemporalUnit unit)
    {
        this.amount = amount;
        this.unit = unit;
    }

    public long amount()
    {
        return amount;
    }

    public TemporalUnit unit()
    {
        return unit;
    }

    private static final Pattern DURATION_PATTERN =
            Pattern.compile("^\\s*(-?\\d+)\\s*([a-zA-Z]+)\\s*$");

    private static final Map<String, TemporalUnit> UNITS = Map.ofEntries(
            Map.entry("ms", ChronoUnit.MILLIS),
            Map.entry("s", ChronoUnit.SECONDS),
            Map.entry("m", ChronoUnit.MINUTES),
            Map.entry("h", ChronoUnit.HOURS),
            Map.entry("d", ChronoUnit.DAYS),
            Map.entry("w", ChronoUnit.WEEKS),
            Map.entry("mo", ChronoUnit.MONTHS),
            Map.entry("y", ChronoUnit.YEARS)
    );

    public static DurationValue parseDuration(String input)
    {
        Matcher matcher = DURATION_PATTERN.matcher(input);

        if (!matcher.matches())
            throw new IllegalArgumentException("Invalid duration: " + input);

        long amount = Long.parseLong(matcher.group(1));
        String unitName = matcher.group(2).toLowerCase();

        TemporalUnit unit = UNITS.get(unitName);

        if (unit == null)
            throw new IllegalArgumentException("Unknown duration unit: " + unitName);

        return new DurationValue(amount, unit);
    }
}

