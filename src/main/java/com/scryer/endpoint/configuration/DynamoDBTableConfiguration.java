package com.scryer.endpoint.configuration;

import com.scryer.endpoint.service.folder.Folder;
import com.scryer.endpoint.service.imagerankings.ImageRanking;
import com.scryer.endpoint.service.imagesrc.ImageSrc;
import com.scryer.endpoint.service.rankingstep.RankingStep;
import com.scryer.endpoint.service.tag.TagModel;
import com.scryer.endpoint.service.userdetails.UserAccessModel;
import com.scryer.endpoint.service.user.User;
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
    public DynamoDbTable<User> userTable(final DynamoDbEnhancedClient client) {
        var projection = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
        var userIdIndex = EnhancedGlobalSecondaryIndex.builder()
                .indexName("userId_index")
                .projection(projection)
                .build();
        var emailIndex = EnhancedGlobalSecondaryIndex.builder().indexName("email_index").projection(projection).build();

        var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex, emailIndex).build();
        return TableInitializer.getOrCreateTable(client, User.class, request);
    }

    @Bean
    public DynamoDbTable<Folder> folderTable(final DynamoDbEnhancedClient client) {
        var projection = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
        var userIdIndex = EnhancedGlobalSecondaryIndex.builder()
                .indexName("userId_index")
                .projection(projection)
                .build();

        var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex).build();
        return TableInitializer.getOrCreateTable(client, Folder.class, request);
    }

    @Bean
    public DynamoDbTable<ImageSrc> imageSrcTable(final DynamoDbEnhancedClient client) {
        var projection = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
        var folderIndex = EnhancedGlobalSecondaryIndex.builder()
                .indexName("folder_index")
                .projection(projection)
                .build();
        var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(folderIndex).build();
        return TableInitializer.getOrCreateTable(client, ImageSrc.class, request);
    }

    @Bean
    public DynamoDbTable<ImageRanking> imageRankingTable(final DynamoDbEnhancedClient client) {
        var projection = Projection.builder().projectionType(ProjectionType.INCLUDE).nonKeyAttributes("id").build();
        var userIdIndex = EnhancedGlobalSecondaryIndex.builder().indexName("userId_index").projection(projection)
                .build();
        var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex).build();
        return TableInitializer.getOrCreateTable(client, ImageRanking.class, request);
    }

    @Bean
    public DynamoDbTable<RankingStep> rankingStepTable(final DynamoDbEnhancedClient client) {
        var projection = Projection.builder().projectionType(ProjectionType.INCLUDE).nonKeyAttributes("id").build();
        var userIdIndex = EnhancedGlobalSecondaryIndex.builder().indexName("userId_index").projection(projection)
                .build();
        var imageRankingIndex = EnhancedGlobalSecondaryIndex.builder()
                .indexName("imageRankingId_index")
                .projection(projection)
                .build();
        var request = CreateTableEnhancedRequest.builder()
                .globalSecondaryIndices(userIdIndex, imageRankingIndex)
                .build();
        return TableInitializer.getOrCreateTable(client, RankingStep.class, request);
    }

    @Bean
    public DynamoDbTable<TagModel> tagTable(final DynamoDbEnhancedClient client) {
        var projection = Projection.builder()
                .projectionType(ProjectionType.INCLUDE)
                .nonKeyAttributes("id", "name")
                .build();
        var userIdIndex = EnhancedGlobalSecondaryIndex.builder()
                .indexName("userId_index")
                .projection(projection)
                .build();
        var tagIndex = EnhancedGlobalSecondaryIndex.builder().indexName("tag_index").projection(projection).build();

        var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex, tagIndex).build();
        return TableInitializer.getOrCreateTable(client, TagModel.class, request);
    }

    @Bean
    public DynamoDbTable<UserAccessModel> userAccessTable(final DynamoDbEnhancedClient client) {
        var projection = Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build();
        var userIdIndex = EnhancedGlobalSecondaryIndex.builder()
                .indexName("userId_index")
                .projection(projection)
                .build();
        var emailIndex = EnhancedGlobalSecondaryIndex.builder().indexName("email_index").projection(projection).build();

        var request = CreateTableEnhancedRequest.builder().globalSecondaryIndices(userIdIndex, emailIndex).build();
        return TableInitializer.getOrCreateTable(client, UserAccessModel.class, request);
    }

}
