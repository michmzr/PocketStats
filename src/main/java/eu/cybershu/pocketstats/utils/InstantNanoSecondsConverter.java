package eu.cybershu.pocketstats.utils;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.time.Instant;

public class InstantNanoSecondsConverter extends StdConverter<String, Instant> {
    @Override
    public Instant convert(String value) {
        return Instant.parse(value);
    }
}
