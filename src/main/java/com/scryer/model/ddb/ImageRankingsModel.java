package com.scryer.model.ddb;

import com.scryer.model.ddb.converters.EloConverter;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.internal.converter.attribute.MapAttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@DynamoDbImmutable(builder = ImageRankingsModel.ImageRankingsModelBuilder.class)
public final class ImageRankingsModel implements DynamoDBTableModel, HasId {
    private final Long id;
    private final Long userId;
    private final String name;
    private final Long lastModified;
    private final Long createDate;
    private final List<Long> imageIds;
    private final Map<Long, Elo> eloMap;

    @DynamoDbPartitionKey
    @DynamoDbSecondarySortKey(indexNames = {"delete_index"})
    public Long getId() {
        return this.id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"user_index"})
    public Long getUserId() {
        return this.userId;
    }

    @DynamoDbConvertedBy(EloConverter.class)
    public Map<Long, Elo> getEloMap() {
        return this.eloMap;
    }
}
