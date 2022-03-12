package com.scryer.endpoint.service.imagesrc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ImageService {

    private final ImageDBAdapter imageDBAdapter;

    @Autowired
    public ImageService(final ImageDBAdapter imageDBAdapter) {
        this.imageDBAdapter = imageDBAdapter;
    }

    public Mono<ImageSrcModel> getImageSrc(final String imageSrcId) {
        return imageDBAdapter.getImageSrc(imageSrcId);
    }

    public Flux<ImageSrcModel> getImageSrcForFolder(final String folderId) {
        return imageDBAdapter.getImageSrcForFolder(folderId);
    }

    public Mono<ImageSrcModel> updateImageSrc(final ImageSrcModel imageSrc) {
        return imageDBAdapter.updateImageSrc(imageSrc);
    }

    public Mono<ImageSrcModel> addImageSrc(final ImageSrcModel imageSrc) {
        return imageDBAdapter.addImageSrc(imageSrc);
    }

    public Mono<ImageSrcModel> deleteImage(final ImageSrcModel imageSrc) {
        return imageDBAdapter.deleteImage(imageSrc);
    }

    public Mono<String> getUniqueId() {
        return imageDBAdapter.getUniqueId();
    }
}
