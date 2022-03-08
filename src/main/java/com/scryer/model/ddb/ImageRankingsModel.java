package com.scryer.model.ddb;

import com.scryer.model.ddb.converters.EloConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@DynamoDbImmutable(builder = ImageRankingsModel.ImageRankingsModelBuilder.class)
public final class ImageRankingsModel implements DynamoDBTableModel, HasId {
    private final String id;
    private final String userId;
    private final String name;
    private final Long lastModified;
    private final Long createDate;
    private final List<String> imageIds;
    private final Map<String, Elo> eloMap;
    private final List<String> tags;

    @DynamoDbPartitionKey
    public String getId() {
        return this.id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"userId_index"})
    public String getUserId() {
        return this.userId;
    }

    @DynamoDbConvertedBy(EloConverter.class)
    public Map<String, Elo> getEloMap() {
        return this.eloMap;
    }
}
