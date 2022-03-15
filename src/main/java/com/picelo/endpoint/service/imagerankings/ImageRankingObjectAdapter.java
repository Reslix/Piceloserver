package com.picelo.endpoint.service.imagerankings;

import reactor.core.publisher.Mono;

interface ImageRankingObjectAdapter {
    Mono<String> uploadRankingState(final String imageRankingId, final String state);

    Mono<String> deleteImage(final String path);
}
