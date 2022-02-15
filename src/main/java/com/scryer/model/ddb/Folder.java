package com.scryer.model.ddb;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;

@Getter
@Builder
@DynamoDbImmutable(builder = Folder.FolderBuilder.class)
public class Folder {
    private long id;
    private long userId;
    private BaseIdentifier source;
    private String name;
    private long createDate;
    private long lastModified;
    private List<String> tags;
    private List<Long> folders;
    private List<Long> parentFolderIds;

    @DynamoDbPartitionKey
    public long getId() {
        return this.id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"user_id_index"})
    public long getUserId() {
        return this.userId;
    }

    @DynamoDb
}
