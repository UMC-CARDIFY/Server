package com.umc.cardify.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.bson.types.ObjectId;

@Converter
public class ObjectIdConverter implements AttributeConverter<ObjectId, String> {
    @Override
    public String convertToDatabaseColumn(ObjectId attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public ObjectId convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new ObjectId(dbData);
    }
}
