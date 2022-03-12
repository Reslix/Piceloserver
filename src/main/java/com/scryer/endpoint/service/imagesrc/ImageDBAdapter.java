package com.scryer.endpoint.service.imagesrc;

import com.scryer.util.IdGenerator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

interface ImageDBAdapter {
    Mono<ImageSrcModel> getImageSrc(final String imageSrcId);

    Flux<ImageSrcModel> getImageSrcForFolder(final String folderId);

    Mono<ImageSrcModel> updateImageSrc(final ImageSrcModel imageSrc);

    Mono<ImageSrcModel> addImageSrc(final ImageSrcModel imageSrc);

    Mono<ImageSrcModel> deleteImage(final ImageSrcModel imageSrc);

    Mono<String> getUniqueId();
}
