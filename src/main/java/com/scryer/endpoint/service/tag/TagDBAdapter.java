package com.scryer.endpoint.service.tag;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface TagDBAdapter {
    Mono<String> getUniqueId();
    Mono<TagModel> addTag(final TagModel tagModel);

    Mono<TagModel> getTag(final String name, final String userId);

    Flux<TagModel> getTags(final String userId);

    Mono<TagModel> updateTag(final TagModel tag);

    Mono<TagModel> deleteTag(final TagModel tag);
}
