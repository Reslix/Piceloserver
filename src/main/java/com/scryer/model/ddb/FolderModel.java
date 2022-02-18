package com.scryer.model.ddb;

import com.scryer.model.ddb.converters.BaseIdentifierConverter;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;

@Getter
@Builder
@DynamoDbImmutable(builder = FolderModel.FolderModelBuilder.class)
public final class FolderModel implements DynamoDBTableModel, HasId {
    private final Long id;
    private final Long userId;
    private final BaseIdentifier source;
    private final String name;
    private final Long createDate;
    private final Long lastModified;
    private final List<String> tags;
    private final List<Long> folders;
    private final List<Long> parentFolderIds;
    private final String state;

    @DynamoDbPartitionKey
    @DynamoDbSecondarySortKey(indexNames = {"user_index", "delete_index"})
    public Long getId() {
        return this.id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"user_index"})
    public Long getUserId() {
        return this.userId;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"delete_index"})
    public String getState() {
        return this.state;
    }

    @DynamoDbConvertedBy(BaseIdentifierConverter.class)
    public BaseIdentifier getSource() {
        return this.source;
    }

}
