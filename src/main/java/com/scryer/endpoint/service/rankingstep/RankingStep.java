package com.scryer.endpoint.service.imagerankingservice;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonSerialize
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public record RankingStep(String id, String userId, String name, String dataSource, List<StepDatum> dataTarget,
		String meta) {
}
