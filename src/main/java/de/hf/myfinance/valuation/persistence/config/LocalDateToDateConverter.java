package de.hf.myfinance.valuation.persistence.config;

import org.springframework.core.convert.converter.Converter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;

public class LocalDateToDateConverter implements Converter<LocalDate, Date> {

    @Override
    public Date convert(LocalDate source) {
        return Date.from(source.atStartOfDay(ZoneOffset.UTC).toInstant());
    }
}