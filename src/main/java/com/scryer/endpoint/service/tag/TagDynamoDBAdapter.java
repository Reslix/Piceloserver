package com.scryer.endpoint.service.tag;

import com.scryer.util.ConsolidateUtil;
import com.scryer.util.IdGenerator;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.List;
import java.util.stream.Stream;

@Service
class TagDynamoDBAdapter implements TagDBAdapter {

    private final DynamoDbTable<TagModel> tagTable;
    private final ReactiveRedisTemplate<String, TagModel> tagRedisTemplate;

    TagDynamoDBAdapter(final DynamoDbTable<TagModel> tagTable,
                       final ReactiveRedisTemplate<String, TagModel> tagRedisTemplate) {
        this.tagTable = tagTable;
        this.tagRedisTemplate = tagRedisTemplate;
    }

    @Override
    public Mono<String> getUniqueId() {
        return Mono.just(IdGenerator.uniqueIdForTable(tagTable, true));
    }

    @Override
    public Mono<TagModel> addTag(final TagModel tagModel) {
        return Mono.justOrEmpty(tagTable.putItemWithResponse(PutItemEnhancedRequest.builder(TagModel.class)
                                                                     .item(tagModel)
                                                                     .build()).attributes())
                .defaultIfEmpty(tagTable.getItem(Key.builder().partitionValue(tagModel.getId()).build()));
    }

    @Override
    public Mono<TagModel> getTag(final String name, final String userId) {
        return tagRedisTemplate.opsForHash().get(userId + name, name).cast(TagModel.class)
                .switchIfEmpty(Mono.defer(() -> {
                    var queryConditional = QueryConditional
                            .keyEqualTo(Key.builder().partitionValue(name).sortValue(userId).build());

                    var tagMono = Mono
                            .justOrEmpty(tagTable.index("tag_index")
                                                 .query(QueryEnhancedRequest.builder()
                                                                .queryConditional(queryConditional)
                                                                .build())
                                                 .stream()
                                                 .flatMap(page -> page.items().stream())
                                                 .findFirst()
                                                 .map(tagTable::getItem))
                            .cache();
                    return tagMono.map(tag -> tagRedisTemplate.opsForHash().put(userId + name, name, tag))
                            .then(tagMono);
                }));
    }

    @Override
    public Flux<TagModel> getTags(final String userId) {
        var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build());
        var tagFlux = Mono.justOrEmpty(tagTable.index("userId_index")
                                               .query(QueryEnhancedRequest.builder()
                                                              .queryConditional(queryConditional)
                                                              .build()).stream().flatMap(page -> page.items().stream()))
                .flatMapIterable(Stream::toList);
        return tagFlux.flatMap(tag -> getTag(tag.getName(), userId));
    }

    @Override
    public Mono<TagModel> updateTag(final TagModel tag) {
        var newTag = Mono.just(tagTable.updateItemWithResponse(
                        UpdateItemEnhancedRequest.builder(TagModel.class)
                                .item(tag)
                                .ignoreNulls(true)
                                .build())
                                       .attributes()).cache();
        return newTag.map(t -> tagRedisTemplate.opsForHash().put(t.getUserId() + t.getName(), t.getName(), t))
                .then(newTag);
    }

    @Override
    public Mono<TagModel> deleteTag(final TagModel tag) {
        return tagRedisTemplate.opsForHash()
                .delete(tag.getUserId() + tag.getName())
                .thenReturn(tag)
                .map(tagTable::deleteItem);
    }
}
