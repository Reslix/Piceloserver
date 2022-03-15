package com.picelo.endpoint.service.tag;

import com.picelo.endpoint.service.DynamoDBTableModel;
import com.picelo.endpoint.service.HasId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.util.List;

@Getter
@Builder
@ToString
@AllArgsConstructor
@DynamoDbImmutable(builder = TagModel.TagModelBuilder.class)
public final class TagModel implements DynamoDBTableModel, HasId {

	private final String id;

	private final String userId;

	private final String name;

	private final List<String> imageIds;

	private final List<String> imageRankingIds;

	@DynamoDbPartitionKey
	public String getId() {
		return this.id;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = { "userId_index" })
	@DynamoDbSecondarySortKey(indexNames = { "tag_index" })
	public String getUserId() {
		return this.userId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = { "tag_index" })
	@DynamoDbSecondarySortKey(indexNames = { "user_index" })
	public String getName() {
		return this.name;
	}

}
