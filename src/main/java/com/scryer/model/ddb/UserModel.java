package com.scryer.model.ddb;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;

@Getter
@Builder
@ToString
@DynamoDbImmutable(builder = UserModel.UserModelBuilder.class)
public final class UserModel implements DynamoDBTableModel, HasId, HasTags {
    private final String id;
    private final String username;
    private final String displayName;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final Long lastLogin;
    private final Long createDate;
    private final Long lastModified;
    private final String rootFolderId;
    private final List<String> imageRankingsIds;
    private final List<String> tags;

    @DynamoDbSecondaryPartitionKey(indexNames = {"userId_index"})
    public String getId() {
        return this.id;
    }

    @DynamoDbPartitionKey
    public String getUsername() {
        return this.username;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"email_index"})
    public String getEmail() {
        return this.email;
    }
}
