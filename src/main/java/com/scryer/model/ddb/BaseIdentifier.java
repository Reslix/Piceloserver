package com.scryer.model.ddb;

import lombok.Builder;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;

public record BaseIdentifier(String type, String src) {
}
