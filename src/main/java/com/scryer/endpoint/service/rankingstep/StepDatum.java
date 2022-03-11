package com.scryer.endpoint.service.rankingstep;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record StepDatum(String targetId, String name, Integer rating, String meta) {
}
