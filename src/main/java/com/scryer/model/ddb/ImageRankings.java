package com.scryer.model.ddb;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@DynamoDbImmutable(builder = ImageRankings.ImageRankingsBuilder.class)
public class ImageRankings {
    private final long id;
    private final long userId;
    private final String name;
    private final long createName;
    private final long lastModified;
    private final List<Long> imageIds;
    private final Map<Long, Elo> eloMap;

    @DynamoDbPartitionKey
    public long getId() {
        return this.id;
    }
}
