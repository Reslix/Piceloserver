package com.scryer.endpoint.service.rankingstep;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.Map;

@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record StepDatum(String id, String name, Integer rating, Map<String, String> meta) {
}
