package org.gooru.nucleus.handlers.classes.processors.repositories.activejdbc.converters;

/**
 * Created by ashish on 28/1/16.
 */
public interface ConverterRegistry {
  FieldConverter lookupConverter(String fieldName);
}
