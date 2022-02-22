package com.scryer.model.ddb.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scryer.model.ddb.Elo;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class EloConverter implements AttributeConverter<Map<String, Elo>> {
    private final ObjectMapper mapper = new ObjectMapper();
    private final TypeReference<Map<String, Elo>> typeReference = new TypeReference<>() {
    };

    @Override
    public AttributeValue transformFrom(Map<String, Elo> input) {
        try {
            return AttributeValue.builder().s(mapper.writeValueAsString(input)).build();
        } catch (JsonProcessingException e) {
            return AttributeValue.builder().build();
        }
    }

    @Override
    public Map<String, Elo> transformTo(AttributeValue input) {
        try {
            return mapper.readValue(input.s(), typeReference);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public EnhancedType<Map<String, Elo>> type() {
        return EnhancedType.mapOf(String.class, Elo.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
