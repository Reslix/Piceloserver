package com.scryer.endpoint.service.rankingstep;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.scryer.endpoint.service.DynamoDBTableModel;
import com.scryer.endpoint.service.HasTags;
import com.scryer.endpoint.service.folder.FolderBaseIdentifierConverter;
import com.scryer.endpoint.service.imagesrc.ImageSrcModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;

import java.util.List;
import java.util.Objects;

@Getter
@Builder
@AllArgsConstructor
@ToString
@DynamoDbImmutable(builder = RankingStep.RankingStepBuilder.class)
public final class RankingStep implements DynamoDBTableModel {

	private final String id;

	private final String userId;

	private final String name;

	private final String dataSource;

	private final List<StepDatum> dataTarget;

	private final String meta;

	@DynamoDbConvertedBy(StepDatumConverter.class)
	public List<StepDatum> getDataTarget() {
		return this.dataTarget;
	}

}
