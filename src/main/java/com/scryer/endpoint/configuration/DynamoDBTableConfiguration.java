package com.scryer.endpoint.configuration;

import com.scryer.model.ddb.*;
import com.scryer.model.ddb.TableInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

@Configuration
public class DynamoDBTableConfiguration {

    @Bean
    public DynamoDbTable<UserModel> userTable(final DynamoDbEnhancedClient client) {
        return TableInitializer.getOrCreateTable(client, UserModel.class);
    }

    @Bean
    public DynamoDbTable<FolderModel> folderTable(final DynamoDbEnhancedClient client) {
        return TableInitializer.getOrCreateTable(client, FolderModel.class);
    }

    @Bean
    public DynamoDbTable<ImageSrcModel> imageSrcTable(final DynamoDbEnhancedClient client) {
        return TableInitializer.getOrCreateTable(client, ImageSrcModel.class);
    }

    @Bean
    public DynamoDbTable<ImageRankingsModel> imageRankingsTable(final DynamoDbEnhancedClient client) {
        return TableInitializer.getOrCreateTable(client, ImageRankingsModel.class);
    }

    @Bean
    public DynamoDbTable<TagModel> tagTable(final DynamoDbEnhancedClient client) {
        return TableInitializer.getOrCreateTable(client, TagModel.class);
    }

    @Bean
    public DynamoDbTable<UserSecurityModel> userSecurityTable(final DynamoDbEnhancedClient client) {
        return TableInitializer.getOrCreateTable(client, UserSecurityModel.class);
    }
}
