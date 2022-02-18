package com.scryer.model.ddb;

import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;

@Getter
@Builder
@DynamoDbImmutable(builder = UserModel.UserModelBuilder.class)
public final class UserModel implements DynamoDBTableModel, HasId {
    private final Long id;
    private final String username;
    private final String displayName;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final Long lastLogin;
    private final Long createDate;
    private final Long lastModified;
    private final Long rootFolderId;
    private final List<Long> imageRankingsIds;
    private final List<String> tags;

    @DynamoDbSecondaryPartitionKey(indexNames = {"userId_index"})
    public Long getId() {
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
