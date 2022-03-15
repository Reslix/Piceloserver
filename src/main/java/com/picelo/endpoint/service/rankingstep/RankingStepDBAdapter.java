package com.picelo.endpoint.service.rankingstep;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface RankingStepDBAdapter {

	Mono<String> getUniqueId();

	Mono<RankingStep> createRankingStep(final RankingStep rankingStep);

	Mono<RankingStep> getRankingStepById(final String id);

	Flux<RankingStep> getRankingStepsByImageRankingId(final String id);

	Mono<RankingStep> updateRankingStep(final RankingStep rankingStep);

	Mono<RankingStep> deleteRankingStep(final RankingStep rankingStep);

}
