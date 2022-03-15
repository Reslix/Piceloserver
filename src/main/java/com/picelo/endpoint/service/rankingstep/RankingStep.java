package com.picelo.endpoint.service.rankingstep;

import com.picelo.endpoint.service.DynamoDBTableModel;
import com.picelo.endpoint.service.HasId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@ToString
@DynamoDbImmutable(builder = RankingStep.RankingStepBuilder.class)
public final class RankingStep implements DynamoDBTableModel, HasId {

	private final String id;

	private final String userId;

	private final String imageRankingId;

	private final String name;

	private final String source;

	private final List<StepDatum> target;

	private final Map<String, String> meta;

	@DynamoDbPartitionKey
	public String getId() {
		return this.id;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = {"userId_index"})
	public String getUserId() {
		return this.userId;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = {"imageRankingId_index"})
	public String getImageRankingId() {
		return this.imageRankingId;
	}

	@DynamoDbConvertedBy(StepDatumConverter.class)
	public List<StepDatum> getTarget() {
		return this.target;
	}

}
