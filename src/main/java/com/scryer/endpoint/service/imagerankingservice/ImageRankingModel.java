package com.scryer.endpoint.service.imagerankingservice;

import com.scryer.endpoint.service.DynamoDBTableModel;
import com.scryer.endpoint.service.HasId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@DynamoDbImmutable(builder = ImageRankingModel.ImageRankingModelBuilder.class)
public final class ImageRankingModel implements DynamoDBTableModel, HasId {

	private final String id;

	private final String userId;

	private final String name;

	private final Long lastModified;

	private final Long createDate;

	private final List<String> rankingSteps;

	private final List<String> imageIds;

	private final List<String> tags;

	@DynamoDbPartitionKey
	public String getId() {
		return this.id;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = { "userId_index" })
	public String getUserId() {
		return this.userId;
	}

}
