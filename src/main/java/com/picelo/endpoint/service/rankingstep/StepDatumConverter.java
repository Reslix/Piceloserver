package com.picelo.endpoint.service.rankingstep;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;

public class StepDatumConverter implements AttributeConverter<List<StepDatum>> {

	private final ObjectMapper mapper = new ObjectMapper();

	private final TypeReference<List<StepDatum>> typeReference = new TypeReference<>() {
	};

	@Override
	public AttributeValue transformFrom(List<StepDatum> stepData) {
		try {
			return AttributeValue.builder().s(mapper.writeValueAsString(stepData)).build();
		}
		catch (JsonProcessingException e) {
			return AttributeValue.builder().build();
		}
	}

	@Override
	public List<StepDatum> transformTo(AttributeValue attributeValue) {
		try {
			return mapper.readValue(attributeValue.s(), typeReference);
		}
		catch (JsonProcessingException e) {
			return null;
		}
	}

	@Override
	public EnhancedType<List<StepDatum>> type() {
		return (EnhancedType<List<StepDatum>>) EnhancedType.of(typeReference.getType());
	}

	@Override
	public AttributeValueType attributeValueType() {
		return AttributeValueType.S;
	}

}
