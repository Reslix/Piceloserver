package com.scryer.model.ddb.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scryer.model.ddb.BaseIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class AuthoritiesConverter implements AttributeConverter<Collection<? extends GrantedAuthority>> {
    @Autowired
    private ObjectMapper mapper;

    private final TypeReference<Collection<? extends GrantedAuthority>> typeReference = new TypeReference<>() {
    };

    @Override
    public AttributeValue transformFrom(Collection<? extends GrantedAuthority> input) {
        try {
            return AttributeValue.builder().s(mapper.writeValueAsString(input)).build();
        } catch (JsonProcessingException e) {
            return AttributeValue.builder().build();
        }
    }

    @Override
    public Collection<? extends GrantedAuthority> transformTo(AttributeValue input) {
        try {
            return mapper.readValue(input.s(), typeReference);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public EnhancedType<Collection<? extends GrantedAuthority>> type() {
        return (EnhancedType<Collection<? extends GrantedAuthority>>) EnhancedType.of(typeReference.getType());
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
