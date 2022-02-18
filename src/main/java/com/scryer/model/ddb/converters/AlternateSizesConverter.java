package com.scryer.model.ddb.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scryer.model.ddb.BaseIdentifier;
import com.scryer.model.ddb.Elo;
import org.springframework.beans.factory.annotation.Autowired;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class AlternateSizesConverter implements AttributeConverter<Map<String, BaseIdentifier>> {
    @Autowired
    private ObjectMapper mapper;

    private final TypeReference<Map<String, BaseIdentifier>> typeReference = new TypeReference<>() {
    };

    @Override
    public AttributeValue transformFrom(Map<String, BaseIdentifier> input) {
        try {
            return AttributeValue.builder().s(mapper.writeValueAsString(input)).build();
        } catch (JsonProcessingException e) {
            return AttributeValue.builder().build();
        }
    }

    @Override
    public Map<String, BaseIdentifier> transformTo(AttributeValue input) {
        try {
            return mapper.readValue(input.s(), typeReference);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public EnhancedType<Map<String, BaseIdentifier>> type() {
        return EnhancedType.mapOf(String.class, BaseIdentifier.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
