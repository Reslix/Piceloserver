package com.scryer.model.ddb;

import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.util.List;

@Getter
@Builder
@DynamoDbImmutable(builder = TagModel.TagModelBuilder.class)
public final class TagModel implements DynamoDBTableModel {
    private final Long id;
    private final Long userId;
    private final String name;
    private final List<Long> imageIds;
    private final List<Long> folderIds;

    @DynamoDbPartitionKey
    public Long getId() {
        return this.id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"user_index"})
    @DynamoDbSecondarySortKey(indexNames = {"tag_index"})
    public Long getUserId() {
        return this.userId;
    }

    @DynamoDbSecondarySortKey(indexNames = {"user_index"})
    @DynamoDbSecondaryPartitionKey(indexNames = {"tag_index"})
    public String getName() {
        return this.name;
    }
}
