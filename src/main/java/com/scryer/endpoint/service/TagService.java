package com.scryer.endpoint.service;

import com.scryer.model.ddb.TagModel;
import com.scryer.util.IdGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TagService {
    private final DynamoDbTable<TagModel> tagTable;

    public TagService(final DynamoDbTable<TagModel> tagTable) {
        this.tagTable = tagTable;
    }

    public Mono<TagModel> getTag(final String name, final String userId) {
        var queryConditional = QueryConditional.keyEqualTo(Key.builder()
                                                                   .partitionValue(name)
                                                                   .sortValue(userId)
                                                                   .build());
        return Mono.justOrEmpty(tagTable.index("tag_index")
                                        .query(QueryEnhancedRequest.builder()
                                                       .queryConditional(queryConditional)
                                                       .attributesToProject()
                                                       .build())
                                        .stream()
                                        .flatMap(page -> page.items().stream())
                                        .findFirst().map(tagTable::getItem));
    }

    public Mono<TagModel> addNewTag(final String name, final String userId) {
        var id = IdGenerator.uniqueIdForTable(tagTable, true);
        return Mono.justOrEmpty(tagTable.putItemWithResponse(PutItemEnhancedRequest.builder(TagModel.class)
                                                                     .item(TagModel.builder()
                                                                                   .id(id)
                                                                                   .name(name)
                                                                                   .imageIds(List.of())
                                                                                   .imageRankingIds(List.of())
                                                                                   .userId(userId)
                                                                                   .build()).build())
                                        .attributes())
                .defaultIfEmpty(tagTable.getItem(Key.builder().partitionValue(id).build()));

    }

    public Mono<TagModel> updateTagImageIds(final TagModel tag, final List<String> imageIds) {
        var tagsIds = tag.getImageIds();
        Set<String> ids = new HashSet<>(tagsIds.size() + 1);
        ids.addAll(tagsIds);
        ids.addAll(imageIds);
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

    public Mono<TagModel> deleteTagImages(final TagModel tag, final List<String> imageId) {
        var tagsIds = tag.getImageIds();
        Set<String> ids = new HashSet<>(tagsIds.size());
        ids.addAll(tagsIds);
        imageId.forEach(ids::remove);

        var newTagModel =
                TagModel.builder()
                        .id(tag.getId())
                        .imageIds(List.copyOf(ids))
                        .build();
        if (ids.size() > 0 || tag.getImageRankingIds().size() > 0) {
            return Mono.just(tagTable.updateItemWithResponse(UpdateItemEnhancedRequest.builder(TagModel.class)
                                                                     .item(newTagModel)
                                                                     .ignoreNulls(true)
                                                                     .build())
                                     .attributes());
        }
        else {
            return Mono.just(tagTable.updateItemWithResponse(UpdateItemEnhancedRequest.builder(TagModel.class)
                                                                     .item(newTagModel)
                                                                     .ignoreNulls(true)
                                                                     .build())
                                     .attributes())
                    .flatMap(this::deleteTag);
        }
    }

}
