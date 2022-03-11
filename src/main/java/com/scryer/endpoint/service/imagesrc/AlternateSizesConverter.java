package com.scryer.endpoint.service.imagesrc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public class AlternateSizesConverter implements AttributeConverter<Map<String, ImageBaseIdentifier>> {

	private final ObjectMapper mapper = new ObjectMapper();

	private final TypeReference<Map<String, ImageBaseIdentifier>> typeReference = new TypeReference<>() {
	};

	@Override
	public AttributeValue transformFrom(Map<String, ImageBaseIdentifier> input) {
		try {
			return AttributeValue.builder().s(mapper.writeValueAsString(input)).build();
		}
		catch (JsonProcessingException e) {
			return AttributeValue.builder().build();
		}
	}

	@Override
	public Map<String, ImageBaseIdentifier> transformTo(AttributeValue input) {
		try {
			return mapper.readValue(input.s(), typeReference);
		}
		catch (JsonProcessingException e) {
			return null;
		}
	}

	@Override
	public EnhancedType<Map<String, ImageBaseIdentifier>> type() {
		return EnhancedType.mapOf(String.class, ImageBaseIdentifier.class);
	}

	@Override
	public AttributeValueType attributeValueType() {
		return AttributeValueType.S;
	}

}
