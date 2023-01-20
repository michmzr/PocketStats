package eu.cybershu.pocketstats.utils;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.time.Instant;

public class LongToInstantConverter extends StdConverter<Long, Instant> {
    public Instant convert(final Long value) {
        if(value == null || value == 0)
            return null;
        else
            return Instant.ofEpochSecond(value);
    }
}