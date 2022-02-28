package com.scryer.endpoint.service;

import com.scryer.model.ddb.TagModel;
import com.scryer.util.IdGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TagService {
    private final DynamoDbTable<TagModel> tagTable;

    public TagService(final DynamoDbTable<TagModel> tagTable) {
        this.tagTable = tagTable;
    }

    public Mono<TagModel> getTagByNameAndUserId(final String name, final String userId) {
        var expression = Expression.builder()
                .expression("name = :name")
                .putExpressionValue(":name", AttributeValue.builder().s(name).build())
                .build();
        var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build());
        return Mono.just(tagTable.query(QueryEnhancedRequest.builder()
                                                .queryConditional(queryConditional)
                                                .filterExpression(expression)
                                                .attributesToProject()
                                                .build())
                                 .items()
                                 .iterator()
                                 .next());
    }

    public Mono<TagModel> addNewTag(final String name, final String userId) {
        var id = IdGenerator.uniqueIdForTable(tagTable, true);
        return Mono.fromCallable(() -> {
            tagTable.putItemWithResponse(PutItemEnhancedRequest.builder(TagModel.class)
                                                 .item(TagModel.builder()
                                                               .id(id)
                                                               .name(name)
                                                               .folderIds(List.of())
                                                               .imageIds(List.of())
                                                               .userId(userId)
                                                               .build()).build());
            return tagTable.getItem(Key.builder().partitionValue(id).build());
        });
    }

    public Mono<TagModel> updateTagFolderId(final TagModel tag, final String folderId) {
        var tagsIds = tag.getFolderIds();
        Set<String> ids = new HashSet<>(tagsIds.size() + 1);
        ids.addAll(tagsIds);
        ids.add(folderId);
        var newTagModel =
                TagModel.builder()
                        .id(tag.getId())
                        .folderIds(List.copyOf(ids))
                        .build();
        return Mono.just(tagTable.updateItemWithResponse(UpdateItemEnhancedRequest.builder(TagModel.class)
                                                                 .item(newTagModel)
                                                                 .ignoreNulls(true)
                                                                 .build())
                                 .attributes());
    }

    public Mono<TagModel> updateTagImageId(final TagModel tag, final String imageId) {
        var tagsIds = tag.getFolderIds();
        Set<String> ids = new HashSet<>(tagsIds.size() + 1);
        ids.addAll(tagsIds);
        ids.add(imageId);
        var newTagModel =
                TagModel.builder()
                        .id(tag.getId())
                        .imageIds(List.copyOf(ids))
                        .build();
        return Mono.just(tagTable.updateItemWithResponse(UpdateItemEnhancedRequest.builder(TagModel.class)
                                                                 .item(newTagModel)
                                                                 .ignoreNulls(true)
                                                                 .build())
                                 .attributes());
    }

    public Mono<TagModel> deleteTag(final TagModel tag) {
        return Mono.justOrEmpty(tagTable.deleteItem(tag));
    }

    public Mono<TagModel> deleteFolderTag(final TagModel tag, final String folderId) {
        var tagsIds = tag.getFolderIds();
        Set<String> ids = new HashSet<>(tagsIds.size());
        ids.addAll(tagsIds);
        ids.remove(folderId);
        var newTagModel =
                TagModel.builder()
                        .id(tag.getId())
                        .folderIds(List.copyOf(ids))
                        .build();
        return Mono.just(tagTable.updateItemWithResponse(UpdateItemEnhancedRequest.builder(TagModel.class)
                                                                 .item(newTagModel)
                                                                 .ignoreNulls(true)
                                                                 .build())
                                 .attributes());
    }

    public Mono<TagModel> deleteImageTag(final TagModel tag, final String imageId) {
        var tagsIds = tag.getFolderIds();
        Set<String> ids = new HashSet<>(tagsIds.size());
        ids.addAll(tagsIds);
        ids.remove(imageId);
        var newTagModel =
                TagModel.builder()
                        .id(tag.getId())
                        .imageIds(List.copyOf(ids))
                        .build();
        return Mono.just(tagTable.updateItemWithResponse(UpdateItemEnhancedRequest.builder(TagModel.class)
                                                                 .item(newTagModel)
                                                                 .ignoreNulls(true)
                                                                 .build())
                                 .attributes());
    }

}
