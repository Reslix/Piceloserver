package com.scryer.endpoint.service.folder;

import com.scryer.endpoint.service.DynamoDBTableModel;
import com.scryer.endpoint.service.HasId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

import java.util.List;

@Getter
@Builder
@ToString
@AllArgsConstructor
@DynamoDbImmutable(builder = Folder.FolderModelBuilder.class)
public final class Folder implements DynamoDBTableModel, HasId {

	private final String id;

	private final String userId;

	private final FolderBaseIdentifier source;

	private final String name;

	private final Long createDate;

	private final Long lastModified;

	private final List<String> folders;

	private final List<String> parentFolderIds;

	@DynamoDbPartitionKey
	public String getId() {
		return this.id;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = { "userId_index" })
	public String getUserId() {
		return this.userId;
	}

	@DynamoDbConvertedBy(FolderBaseIdentifierConverter.class)
	public FolderBaseIdentifier getSource() {
		return this.source;
	}

}
