package com.scryer.endpoint.service;

import com.scryer.model.ddb.TagModel;
import com.scryer.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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
    private final ReactiveRedisTemplate<String, TagModel> tagRedisTemplate;

    @Autowired
    public TagService(final DynamoDbTable<TagModel> tagTable,
                      final ReactiveRedisTemplate<String, TagModel> tagRedisTemplate) {
        this.tagTable = tagTable;
        this.tagRedisTemplate = tagRedisTemplate;
    }

    public Mono<TagModel> getTag(final String name, final String userId) {
        return tagRedisTemplate.opsForHash().get(userId + name, name).cast(TagModel.class)
                .switchIfEmpty(Mono.defer(() -> {
                    var queryConditional = QueryConditional.keyEqualTo(Key.builder()
                                                                               .partitionValue(name)
                                                                               .sortValue(userId)
                                                                               .build());

                    var tagMono = Mono.justOrEmpty(tagTable.index("tag_index")
                                                           .query(QueryEnhancedRequest.builder()
                                                                          .queryConditional(queryConditional)
                                                                          .attributesToProject()
                                                                          .build())
                                                           .stream()
                                                           .flatMap(page -> page.items().stream())
                                                           .findFirst().map(tagTable::getItem)).cache();
                    return tagMono.map(tag -> tagRedisTemplate.opsForHash().put(userId + name, name, tag)).then(tagMono);
                }));
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
        var newTag = Mono.just(tagTable.updateItemWithResponse(UpdateItemEnhancedRequest.builder(TagModel.class)
                                                                       .item(newTagModel)
                                                                       .ignoreNulls(true)
                                                                       .build())
                                       .attributes())
                .cache();
        return newTag.map(t -> tagRedisTemplate.opsForHash().put(t.getUserId() + t.getName(), t.getName(), t)).then(newTag);
    }


    public Mono<TagModel> deleteTag(final TagModel tag) {
        return tagRedisTemplate.opsForHash().delete(tag.getUserId() + tag.getName()).then(Mono.justOrEmpty(tagTable.deleteItem(tag)));
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
            var newTag = Mono.just(tagTable.updateItemWithResponse(UpdateItemEnhancedRequest.builder(TagModel.class)
                                                                           .item(newTagModel)
                                                                           .ignoreNulls(true)
                                                                           .build())
                                           .attributes());
            return newTag.map(t -> tagRedisTemplate.opsForHash().put(t.getUserId() + t.getName(), t.getName(), t)).then(newTag);

        } else {
            var newTag = Mono.just(tagTable.updateItemWithResponse(UpdateItemEnhancedRequest.builder(TagModel.class)
                                                                           .item(newTagModel)
                                                                           .ignoreNulls(true)
                                                                           .build())
                                           .attributes()).cache();

            return newTag.flatMap(this::deleteTag)
                    .then(newTag)
                    .map(t -> tagRedisTemplate.opsForHash().delete(t.getUserId() + t.getName()))
                    .then(newTag);
        }
    }

}
