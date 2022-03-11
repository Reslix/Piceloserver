package com.scryer.endpoint.service.userdetails;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.springframework.security.core.GrantedAuthority;

import java.io.IOException;

public class GrantedAuthoritySerializer extends StdSerializer<GrantedAuthority> {

	public GrantedAuthoritySerializer() {
		this(null);
	}

	protected GrantedAuthoritySerializer(Class<GrantedAuthority> t) {
		super(t);
	}

	@Override
	public void serialize(GrantedAuthority value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {
		gen.writeRawValue(value.getAuthority());
	}

}
