package com.scryer.endpoint.service.imagesrc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class ImageBaseIdentifierConverter implements AttributeConverter<ImageBaseIdentifier> {

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public AttributeValue transformFrom(ImageBaseIdentifier input) {
		try {
			return AttributeValue.builder().s(mapper.writeValueAsString(input)).build();
		}
		catch (JsonProcessingException e) {
			return AttributeValue.builder().build();
		}
	}

	@Override
	public ImageBaseIdentifier transformTo(AttributeValue input) {
		try {
			return mapper.readValue(input.s(), ImageBaseIdentifier.class);
		}
		catch (JsonProcessingException e) {
			return null;
		}
	}

	@Override
	public EnhancedType<ImageBaseIdentifier> type() {
		return EnhancedType.of(ImageBaseIdentifier.class);
	}

	@Override
	public AttributeValueType attributeValueType() {
		return AttributeValueType.S;
	}

}
