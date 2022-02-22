package com.scryer.model.ddb;

import com.scryer.model.ddb.converters.AlternateSizesConverter;
import com.scryer.model.ddb.converters.BaseIdentifierConverter;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@DynamoDbImmutable(builder = ImageSrcModel.ImageSrcModelBuilder.class)
public final class ImageSrcModel implements DynamoDBTableModel, HasId, HasTags {
    private final String id;
    private final String userId;
    private final BaseIdentifier source;
    private final String name;
    private final Long createDate;
    private final Long lastModified;
    private final String type;
    private final String size;
    private final Map<String, BaseIdentifier> alternateSizes;
    private final List<String> tags;
    private final String parentFolderId;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"folder_index"})
    public String getParentFolderId() {
        return this.parentFolderId;
    }

    @DynamoDbSecondarySortKey(indexNames = {"folder_index"})
    public String getSize() {
        return this.size;
    }

    @DynamoDbConvertedBy(BaseIdentifierConverter.class)
    public BaseIdentifier getSource() {
        return this.source;
    }

    @DynamoDbConvertedBy(AlternateSizesConverter.class)
    public Map<String, BaseIdentifier> getAlternateSizes() {
        return this.alternateSizes;
    }
}
