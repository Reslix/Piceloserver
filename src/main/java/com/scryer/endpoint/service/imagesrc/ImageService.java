package com.scryer.endpoint.service.imagesrc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ImageService {

    private final ImageDBAdapter imageDBAdapter;

    @Autowired
    public ImageService(final ImageDBAdapter imageDBAdapter) {
        this.imageDBAdapter = imageDBAdapter;
    }

    public Flux<ImageSrc> getImageSrcs(final List<String> imageSrcId) {
        return Flux.fromIterable(imageSrcId).flatMap(imageDBAdapter::getImageSrc);
    }

    public Flux<ImageSrc> getImageSrcForFolder(final String folderId) {
        return imageDBAdapter.getImageSrcForFolder(folderId);
    }

    public Mono<ImageSrc> updateImageSrc(final ImageSrc imageSrc) {
        return imageDBAdapter.updateImageSrc(imageSrc);
    }

    public Mono<ImageSrc> addImageSrc(final ImageSrc imageSrc) {
        return imageDBAdapter.addImageSrc(imageSrc);
    }

    public Mono<ImageSrc> deleteImage(final ImageSrc imageSrc) {
        return imageDBAdapter.deleteImage(imageSrc);
    }

    public Mono<String> getUniqueId() {
        return imageDBAdapter.getUniqueId();
    }
}
