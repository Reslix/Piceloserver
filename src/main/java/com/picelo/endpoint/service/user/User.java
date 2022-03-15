package com.picelo.endpoint.service.user;

import com.picelo.endpoint.service.DynamoDBTableModel;
import com.picelo.endpoint.service.HasId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbImmutable;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;

@Getter
@Builder
@ToString
@AllArgsConstructor
@DynamoDbImmutable(builder = User.UserBuilder.class)
public final class User implements DynamoDBTableModel, HasId {

	private final String id;

	private final String username;

	private final String displayName;

	private final String firstName;

	private final String lastName;

	private final String email;

	private final Long lastLogin;

	private final Long createDate;

	private final Long lastModified;

	private final String rootFolderId;

	@DynamoDbSecondaryPartitionKey(indexNames = { "userId_index" })
	public String getId() {
		return this.id;
	}

	@DynamoDbPartitionKey
	public String getUsername() {
		return this.username;
	}

	@DynamoDbSecondaryPartitionKey(indexNames = { "email_index" })
	public String getEmail() {
		return this.email;
	}

}
