package com.scryer.model.ddb;

import lombok.Builder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public record Comparison(Long imageId1,
                         Long imageId2,
                         Integer magnitude,
                         Long createDate,
                         Map<String, AttributeValue> meta) {
}
