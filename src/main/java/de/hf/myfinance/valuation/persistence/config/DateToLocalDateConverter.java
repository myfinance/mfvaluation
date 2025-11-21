package de.hf.myfinance.valuation.persistence.config;

import org.springframework.core.convert.converter.Converter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

public class DateToLocalDateConverter implements Converter<Date, LocalDate> {

    @Override
    public LocalDate convert(Date source) {
        return source.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
    }
}