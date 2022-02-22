package com.scryer.model.ddb;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.util.List;

@Getter
@Builder
@ToString
@DynamoDbImmutable(builder = TagModel.TagModelBuilder.class)
public final class TagModel implements DynamoDBTableModel, HasId {
    private final String id;
    private final String userId;
    private final String name;
    private final List<String> imageIds;
    private final List<String> folderIds;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"userId_index"})
    public String getUserId() {
        return this.userId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"tag_index"})
    public String getName() {
        return this.name;
    }
}
