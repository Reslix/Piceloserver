package com.scryer.model.ddb;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;

@Getter
@Builder
@DynamoDbImmutable(builder = User.UserBuilder.class)
public class User {
    private final long id;
    private final String username;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final long lastLogin;
    private final long createDate;
    private final long rootFolderId;
    private final long imageRankingsId;
    private final List<String> tags;

    @DynamoDbPartitionKey
    public String getUsername() {
        return this.username;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"email_index"})
    public String getEmail() {
        return this.email;
    }

}
