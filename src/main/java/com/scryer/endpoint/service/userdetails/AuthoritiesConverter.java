package com.scryer.endpoint.service.userdetails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Arrays;
import java.util.Collection;

public class AuthoritiesConverter implements AttributeConverter<Collection<? extends GrantedAuthority>> {

	private final ObjectMapper mapper;

	private final TypeReference<Collection<? extends GrantedAuthority>> typeReference = new TypeReference<>() {
	};

	public AuthoritiesConverter() {
		this.mapper = new ObjectMapper();
		var simpleModule = new SimpleModule();
		simpleModule.addSerializer(GrantedAuthority.class, new GrantedAuthoritySerializer());
	}

	@Override
	public AttributeValue transformFrom(Collection<? extends GrantedAuthority> input) {
		try {
			return AttributeValue.builder().s(mapper.writeValueAsString(input.stream().map(authority -> {
				try {
					return mapper.writeValueAsString(authority);
				}
				catch (JsonProcessingException e) {
					return "";
				}
			}).toArray())).build();
		}
		catch (JsonProcessingException e) {
			return AttributeValue.builder().build();
		}
	}

	@Override
	public Collection<? extends GrantedAuthority> transformTo(AttributeValue input) {
		try {
			return Arrays.stream(mapper.readValue(input.s(), String[].class)).map(SimpleGrantedAuthority::new).toList();
		}
		catch (JsonProcessingException e) {
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
