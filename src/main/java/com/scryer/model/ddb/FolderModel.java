package com.scryer.model.ddb;

import com.scryer.model.ddb.converters.BaseIdentifierConverter;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;

@Getter
@Builder
@ToString
@DynamoDbImmutable(builder = FolderModel.FolderModelBuilder.class)
public final class FolderModel implements DynamoDBTableModel, HasId, HasTags {
    private final String id;
    private final String userId;
    private final BaseIdentifier source;
    private final String name;
    private final Long createDate;
    private final Long lastModified;
    private final List<String> tags;
    private final List<String> folders;
    private final List<String> parentFolderIds;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"userId_index"})
    public String getUserId() {
        return this.userId;
    }


    @DynamoDbConvertedBy(BaseIdentifierConverter.class)
    public BaseIdentifier getSource() {
        return this.source;
    }

}
