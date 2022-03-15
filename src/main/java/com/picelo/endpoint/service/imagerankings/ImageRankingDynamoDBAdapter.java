package com.picelo.endpoint.service.imagerankings;

import com.picelo.util.IdGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

@Service
class ImageRankingDynamoDBAdapter implements ImageRankingDBAdapter {

    private final DynamoDbTable<ImageRanking> imageRankingTable;

    ImageRankingDynamoDBAdapter(final DynamoDbTable<ImageRanking> imageRankingTable) {
        this.imageRankingTable = imageRankingTable;
    }

    @Override
    public Mono<String> getUniqueId() {
        return Mono.fromCallable(() -> IdGenerator.uniqueIdForTable(imageRankingTable, true));
    }

    @Override
    public Mono<ImageRanking> createImageRanking(final ImageRanking imageRanking) {
        return Mono.justOrEmpty(imageRankingTable.putItemWithResponse(PutItemEnhancedRequest.builder(ImageRanking.class)
                                                                              .item(imageRanking)
                                                                              .build()).attributes())
                .switchIfEmpty(getImageRankingById(imageRanking.getId()));
    }

    @Override
    public Mono<ImageRanking> getImageRankingById(final String id) {
        return Mono.justOrEmpty(imageRankingTable.getItem(Key.builder().partitionValue(id).build()));
    }

    @Override
    public Flux<ImageRanking> getImageRankingsForUser(final String userId) {
        return Flux
                .fromStream(imageRankingTable
                                    .index("userId_index")
                                    .query(QueryEnhancedRequest.builder()
                                                   .queryConditional(QueryConditional
                                                                             .keyEqualTo(Key.builder()
                                                                                                 .partitionValue(userId)
                                                                                                 .build()))
                                                   .build())
                                    .stream().flatMap(page -> page.items().stream()))
                .map(ImageRanking::getId).flatMap(this::getImageRankingById);
    }

    @Override
    public Mono<ImageRanking> updateImageRanking(final ImageRanking imageRanking) {
        return Mono.justOrEmpty(imageRankingTable.updateItem(UpdateItemEnhancedRequest.builder(ImageRanking.class)
                                                                     .item(imageRanking)
                                                                     .ignoreNulls(true)
                                                                     .build()));
    }

    @Override
    public Mono<ImageRanking> deleteImageRanking(final ImageRanking imageRanking) {
        return Mono.justOrEmpty(imageRankingTable.deleteItem(imageRanking));
    }

}
