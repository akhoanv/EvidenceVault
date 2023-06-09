package com.risky.jotterbox.dao.converter;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.converter.PropertyConverter;

/**
 * Convert ObjectBox database to {@link List} of {@link String} and vice versa
 *
 * @author Khoa Nguyen
 */
public class StringArrayConverter implements PropertyConverter<List<String>, String>  {
    @Override
    public List<String> convertToEntityProperty(String databaseValue) {
        if (databaseValue == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (String s : databaseValue.split(";")) {
            if (!s.isEmpty()) {
                result.add(s);
            }
        }

        return result;
    }

    @Override
    public String convertToDatabaseValue(List<String> entityProperty) {
        String data = "";
        for (String s : entityProperty) {
            data += s + ";";
        }

        return data;
    }
}
