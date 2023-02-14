package eu.cybershu.pocketstats.utils;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.springframework.util.StringUtils;

public class StringBooleanToBoolean extends StdConverter<String,Boolean> {
    @Override
    public Boolean convert(String value) {
        if(StringUtils.hasLength(value)) {
            if(value.length() > 1) {
                throw new IllegalArgumentException("String boolean cannot be longer than 1 character. Value '" + value + "' is invalid");
            } else {
                return value.equals("1") ? Boolean.TRUE : Boolean.FALSE;
            }
        }else {
            return null;
        }
    }
}
