package com.scryer.endpoint.configuration;

import com.scryer.endpoint.service.folder.FolderModel;
import com.scryer.endpoint.service.imagerankingservice.ImageRankingModel;
import com.scryer.endpoint.service.imagesrc.ImageSrcModel;
import com.scryer.endpoint.service.tag.TagModel;
import com.scryer.endpoint.service.userdetails.UserAccessModel;
import com.scryer.endpoint.service.user.UserModel;
import com.scryer.util.TableInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.EnhancedGlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;

@Configuration
public class DynamoDBTableConfiguration {

	@Bean
	public DynamoDbTable<UserModel> userTable(final DynamoDbEnhancedClient client) {
		var projection = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
		var userIdIndex = EnhancedGlobalSecondaryIndex.builder().indexName("userId_index").projection(projection)
				.build();
		var emailIndex = EnhancedGlobalSecondaryIndex.builder().indexName("email_index").projection(projection).build();

		var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex, emailIndex).build();
		return TableInitializer.getOrCreateTable(client, UserModel.class, request);
	}

	@Bean
	public DynamoDbTable<FolderModel> folderTable(final DynamoDbEnhancedClient client) {
		var projection = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
		var userIdIndex = EnhancedGlobalSecondaryIndex.builder().indexName("userId_index").projection(projection)
				.build();

		var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex).build();
		return TableInitializer.getOrCreateTable(client, FolderModel.class, request);
	}

	@Bean
	public DynamoDbTable<ImageSrcModel> imageSrcTable(final DynamoDbEnhancedClient client) {
		var projection = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
		var folderIndex = EnhancedGlobalSecondaryIndex.builder().indexName("folder_index").projection(projection)
				.build();
		var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(folderIndex).build();
		return TableInitializer.getOrCreateTable(client, ImageSrcModel.class, request);
	}

	@Bean
	public DynamoDbTable<ImageRankingModel> imageRankingTable(final DynamoDbEnhancedClient client) {
		var projection = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
		var userIdIndex = EnhancedGlobalSecondaryIndex.builder().indexName("userId_index").projection(projection)
				.build();

		var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex).build();
		return TableInitializer.getOrCreateTable(client, ImageRankingModel.class, request);
	}

	@Bean
	public DynamoDbTable<TagModel> tagTable(final DynamoDbEnhancedClient client) {
		var projection = Projection.builder().projectionType(ProjectionType.INCLUDE).nonKeyAttributes("id", "name")
				.build();
		var userIdIndex = EnhancedGlobalSecondaryIndex.builder().indexName("userId_index").projection(projection)
				.build();
		var tagIndex = EnhancedGlobalSecondaryIndex.builder().indexName("tag_index").projection(projection).build();

		var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex, tagIndex).build();
		return TableInitializer.getOrCreateTable(client, TagModel.class, request);
	}

	@Bean
	public DynamoDbTable<UserAccessModel> userAccessTable(final DynamoDbEnhancedClient client) {
		var projection = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
		var userIdIndex = EnhancedGlobalSecondaryIndex.builder().indexName("userId_index").projection(projection)
				.build();
		var emailIndex = EnhancedGlobalSecondaryIndex.builder().indexName("email_index").projection(projection).build();

		var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex, emailIndex).build();
		return TableInitializer.getOrCreateTable(client, UserAccessModel.class, request);
	}

}
