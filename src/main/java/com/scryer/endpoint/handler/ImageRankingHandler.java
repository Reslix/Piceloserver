package com.scryer.endpoint.handler;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Service
public class ImageRankingHandler {

    public Mono<ServerResponse> getImageRankings(final ServerResponse serverResponse) {
        return Mono.empty();
    }

    public Mono<ServerResponse> createImageRankings(final ServerResponse serverResponse) {
        return Mono.empty();
    }

    public Mono<ServerResponse> updateRankingStep(final ServerResponse serverResponse) {
        return Mono.empty();
    }
}
