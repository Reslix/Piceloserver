package com.scryer.model.ddb;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

public record Comparison(String imageId1,
                         String imageId2,
                         Integer rating1,
                         Integer rating2,
                         Float magnitude,
                         Long createDate,
                         Map<String, AttributeValue> meta) {
}
