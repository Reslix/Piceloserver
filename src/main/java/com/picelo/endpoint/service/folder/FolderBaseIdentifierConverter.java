package com.picelo.endpoint.service.folder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class FolderBaseIdentifierConverter implements AttributeConverter<FolderBaseIdentifier> {

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public AttributeValue transformFrom(FolderBaseIdentifier input) {
		try {
			return AttributeValue.builder().s(mapper.writeValueAsString(input)).build();
		}
		catch (JsonProcessingException e) {
			return AttributeValue.builder().build();
		}
	}

	@Override
	public FolderBaseIdentifier transformTo(AttributeValue input) {
		try {
			return mapper.readValue(input.s(), FolderBaseIdentifier.class);
		}
		catch (JsonProcessingException e) {
			return null;
		}
	}

	@Override
	public EnhancedType<FolderBaseIdentifier> type() {
		return EnhancedType.of(FolderBaseIdentifier.class);
	}

	@Override
	public AttributeValueType attributeValueType() {
		return AttributeValueType.S;
	}

}
