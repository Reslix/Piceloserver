package com.scryer.endpoint.service.rankingstep;

import com.scryer.util.IdGenerator;
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
class RankingStepDynamoDBAdapter implements RankingStepDBAdapter {

    private final DynamoDbTable<RankingStep> rankingStepTable;

    RankingStepDynamoDBAdapter(final DynamoDbTable<RankingStep> rankingStepTable) {
        this.rankingStepTable = rankingStepTable;
    }

    @Override
    public Mono<String> getUniqueId() {
        return Mono.fromCallable(() -> IdGenerator.uniqueIdForTable(rankingStepTable, true));
    }

    @Override
    public Mono<RankingStep> createRankingStep(final RankingStep rankingStep) {
        return Mono.justOrEmpty(rankingStepTable.putItemWithResponse(PutItemEnhancedRequest.builder(RankingStep.class)
                                                                             .item(rankingStep)
                                                                             .build()).attributes())
                .switchIfEmpty(getRankingStepById(rankingStep.getId()));
    }

    @Override
    public Mono<RankingStep> getRankingStepById(final String id) {
        return Mono.justOrEmpty(rankingStepTable.getItem(Key.builder().partitionValue(id).build()));
    }

    @Override
    public Flux<RankingStep> getRankingStepsByImageRankingId(final String id) {
        var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(id).build());
        var enhancedQuery = QueryEnhancedRequest.builder().queryConditional(queryConditional).build();
        return Flux.fromStream(rankingStepTable.index("imageRankingId_index")
                                       .query(enhancedQuery)
                                       .stream()
                                       .flatMap(page -> page.items()
                                               .stream())).map(rankingStepTable::getItem);
    }

    @Override
    public Mono<RankingStep> updateRankingStep(final RankingStep rankingStep) {
        return Mono.justOrEmpty(rankingStepTable.updateItem(UpdateItemEnhancedRequest.builder(RankingStep.class)
                                                                    .item(rankingStep)
                                                                    .ignoreNulls(true)
                                                                    .build()));
    }

    @Override
    public Mono<RankingStep> deleteRankingStep(final RankingStep rankingStep) {
        return Mono.justOrEmpty(rankingStepTable.deleteItem(rankingStep));
    }

}
