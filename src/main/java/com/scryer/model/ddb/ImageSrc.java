package com.scryer.model.ddb;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@DynamoDbImmutable(builder = ImageSrc.ImageSrcBuilder.class)
public class ImageSrc {
    private final long id;
    private final long userId;
    private final BaseIdentifier source;
    private final String name;
    private final long createDate;
    private final long lastModified;
    private final String type;
    private final String size;
    private final Map<String, BaseIdentifier> alternateSizes;
    private final List<String> tags;
    private final List<Long> parentFolderIds;

    @DynamoDbPartitionKey
    public long getId() {
        return this.id;
    }
}
