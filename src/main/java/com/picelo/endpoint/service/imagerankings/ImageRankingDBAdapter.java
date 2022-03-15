package com.picelo.endpoint.service.imagerankings;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ImageRankingDBAdapter {
	Mono<String> getUniqueId();

	Mono<ImageRanking> createImageRanking(final ImageRanking imageRanking);

	Mono<ImageRanking> getImageRankingById(final String id);

	Flux<ImageRanking> getImageRankingsForUser(final String userId);

	Mono<ImageRanking> updateImageRanking(final ImageRanking imageRanking);

	Mono<ImageRanking> deleteImageRanking(final ImageRanking imageRanking);

}
