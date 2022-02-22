package com.scryer.model.ddb.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scryer.model.ddb.BaseIdentifier;
import lombok.Builder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class BaseIdentifierConverter implements AttributeConverter<BaseIdentifier> {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public AttributeValue transformFrom(BaseIdentifier input) {
        try {
            return AttributeValue.builder().s(mapper.writeValueAsString(input)).build();
        } catch (JsonProcessingException e) {
            return AttributeValue.builder().build();
        }
    }

    @Override
    public BaseIdentifier transformTo(AttributeValue input) {
        try {
            return mapper.readValue(input.s(), BaseIdentifier.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public EnhancedType<BaseIdentifier> type() {
        return EnhancedType.of(BaseIdentifier.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
