package com.scryer.endpoint.service.imagesrc;

import com.scryer.endpoint.service.DynamoDBTableModel;
import com.scryer.endpoint.service.HasId;
import com.scryer.endpoint.service.HasTags;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@DynamoDbImmutable(builder = ImageSrcModel.ImageSrcModelBuilder.class)
public final class ImageSrcModel implements DynamoDBTableModel, HasId {

	private final String id;

	private final String userId;

	private final ImageBaseIdentifier source;

	private final String name;

	private final Long createDate;

	private final Long lastModified;

	private final String type;

	private final String size;
	
	private final List<String> tags;

	private final Map<String, ImageBaseIdentifier> alternateSizes;

	private final String parentFolderId;

	@DynamoDbPartitionKey
	public String getId() {
		return this.id;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = { "folder_index" })
	public String getParentFolderId() {
		return this.parentFolderId;
	}

	@DynamoDbSecondarySortKey(indexNames = { "folder_index" })
	public String getSize() {
		return this.size;
	}

	@DynamoDbConvertedBy(ImageBaseIdentifierConverter.class)
	public ImageBaseIdentifier getSource() {
		return this.source;
	}

	@DynamoDbConvertedBy(AlternateSizesConverter.class)
	public Map<String, ImageBaseIdentifier> getAlternateSizes() {
		return this.alternateSizes;
	}

}
