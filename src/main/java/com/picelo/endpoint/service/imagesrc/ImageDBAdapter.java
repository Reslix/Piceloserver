package com.picelo.endpoint.service.imagesrc;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ImageDBAdapter {
    Mono<ImageSrc> getImageSrc(final String imageSrcId);

    Flux<ImageSrc> getImageSrcForFolder(final String folderId);

    Mono<ImageSrc> updateImageSrc(final ImageSrc imageSrc);

    Mono<ImageSrc> addImageSrc(final ImageSrc imageSrc);

    Mono<ImageSrc> deleteImage(final ImageSrc imageSrc);

    Mono<String> getUniqueId();
}
