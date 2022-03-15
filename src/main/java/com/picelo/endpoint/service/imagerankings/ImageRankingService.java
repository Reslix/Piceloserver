package com.picelo.endpoint.service.imagerankings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ImageRankingService {

    private final ImageRankingDBAdapter imageRankingDBAdapter;
    private final ImageRankingObjectAdapter imageRankingObjectAdapter;

    @Autowired
    public ImageRankingService(final ImageRankingDBAdapter imageRankingDBAdapter,
                               final ImageRankingObjectAdapter imageRankingObjectAdapter) {
        this.imageRankingDBAdapter = imageRankingDBAdapter;
        this.imageRankingObjectAdapter = imageRankingObjectAdapter;
    }

    public Mono<ImageRanking> createImageRanking(final String name,
                                                 final String userId,
                                                 final List<String> images) {
        var uniqueId = imageRankingDBAdapter.getUniqueId();
        var currentTime = System.currentTimeMillis();
        return uniqueId.flatMap(id -> imageRankingDBAdapter.createImageRanking(ImageRanking.builder()
                                                                                       .id(id)
                                                                                       .userId(userId)
                                                                                       .name(name)
                                                                                       .createDate(currentTime)
                                                                                       .lastModified(currentTime)
                                                                                       .imageIds(images)
                                                                                       .build()));

    }

    public Mono<ImageRanking> getImageRankingById(final String id) {
        return imageRankingDBAdapter.getImageRankingById(id);
    }

    public Flux<ImageRanking> getImageRankingsByUser(final String userId) {
        return imageRankingDBAdapter.getImageRankingsForUser(userId);
    }

    public Mono<ImageRanking> updateImageRanking(final ImageRanking imageRanking) {
        return imageRankingDBAdapter.updateImageRanking(imageRanking);
    }

    public Mono<ImageRanking> deleteImageRanking(final ImageRanking imageRanking) {
        return imageRankingDBAdapter.deleteImageRanking(imageRanking);
    }

    public Mono<String> uploadRankingState(final ImageRanking imageRanking, final String state) {
        var pathMono = imageRankingObjectAdapter.uploadRankingState(imageRanking.getId(), state);
        return pathMono.flatMap(path -> updateImageRanking(ImageRanking.builder()
                                                                   .id(imageRanking.getId())
                                                                   .stateUrl(path)
                                                                   .build())).then(pathMono);
    }

    public Mono<String> deleteRankingState(final ImageRanking imageRanking) {
        var pathMono = imageRankingObjectAdapter.deleteImage(imageRanking.getStateUrl());
        return updateImageRanking(ImageRanking.builder()
                                          .id(imageRanking.getId())
                                          .stateUrl("")
                                          .build()).then(pathMono);

    }

}
