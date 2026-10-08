package com.hotel.config;

import com.hotel.util.DateTimeUtil;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Universal flexible LocalDate converter and formatter.
 * Handles ISO (yyyy-MM-dd), slash formats (dd/MM/yy, dd/MM/yyyy),
 * dash formats, and textual month formats without conversion exceptions.
 */
@Component
public class FlexibleLocalDateConverter implements Converter<String, LocalDate>, Formatter<LocalDate> {

    @Override
    public LocalDate convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }
        return DateTimeUtil.parseFlexibleLocalDate(source);
    }

    @Override
    public LocalDate parse(String text, Locale locale) throws ParseException {
        try {
            return convert(text);
        } catch (IllegalArgumentException e) {
            throw new ParseException(e.getMessage(), 0);
        }
    }

    @Override
    public String print(LocalDate object, Locale locale) {
        if (object == null) {
            return "";
        }
        return object.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
