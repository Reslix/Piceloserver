package com.picelo.endpoint.service.imagesrc;

import com.picelo.endpoint.service.DynamoDBTableModel;
import com.picelo.endpoint.service.HasId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@DynamoDbImmutable(builder = ImageSrc.ImageSrcBuilder.class)
public final class ImageSrc implements DynamoDBTableModel, HasId {

	private final String id;

	private final String userId;

	private final ImageBaseIdentifier source;

	private final String name;

	private final Long createDate;

	private final Long lastModified;

	private final String type;

	private final String size;

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
