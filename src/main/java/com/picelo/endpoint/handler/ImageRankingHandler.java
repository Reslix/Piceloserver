package com.picelo.endpoint.handler;

import com.picelo.endpoint.security.JWTManager;
import com.picelo.endpoint.service.imagerankings.ImageRanking;
import com.picelo.endpoint.service.imagerankings.ImageRankingService;
import com.picelo.endpoint.service.rankingstep.RankingStep;
import com.picelo.endpoint.service.rankingstep.RankingStepService;
import com.picelo.endpoint.service.tag.TagService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class ImageRankingHandler {

    private final TagService tagService;
    private final ImageRankingService imageRankingService;
    private final RankingStepService rankingStepService;
    private final JWTManager jwtManager;

    public ImageRankingHandler(final TagService tagService,
                               final ImageRankingService imageRankingService,
                               final RankingStepService rankingStepService,
                               final JWTManager jwtManager) {
        this.tagService = tagService;
        this.imageRankingService = imageRankingService;
        this.rankingStepService = rankingStepService;
        this.jwtManager = jwtManager;
    }

    public Mono<ServerResponse> createImageRanking(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);
        var imageRankingRequestMono = serverRequest.bodyToMono(CreateImageRankingRequest.class).cache();
        var updatedImageRankingMono = imageRankingRequestMono
                .flatMap(request -> imageRankingService.createImageRanking(request.name(),
                                                                           request.userId(),
                                                                           request.imageIds())).cache();
        var updatedTagsFlux = imageRankingRequestMono.flatMapIterable(CreateImageRankingRequest::tags)
                .flatMap(tag -> tagService.getTag(tag, userId.id()))
                .zipWith(updatedImageRankingMono)
                .flatMap(t2 -> tagService.updateTagImageRankingIds(t2.getT1(), List.of(t2.getT2().getId())));

        return updatedTagsFlux.then(updatedImageRankingMono)
                .flatMap(ranking -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(ranking)));
    }

    record CreateImageRankingRequest(String name,
                                     String userId,
                                     List<String> imageIds,
                                     List<String> tags) {
    }

    public Mono<ServerResponse> createRankingsSteps(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);
        return serverRequest.bodyToMono(CreateRankingStepsRequest.class)
                .map(CreateRankingStepsRequest::steps)
                .flatMap(rankingStepService::createRankingSteps)
                .flatMap(steps -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(steps)));
    }

    record CreateRankingStepsRequest(List<RankingStep> steps) {

    }

    public Mono<ServerResponse> getUserImageRankings(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);
        var userToGet = serverRequest.pathVariable("userId");
        return imageRankingService.getImageRankingsByUser(userToGet)
                .collectList()
                .flatMap(imageRankings -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(imageRankings)));
    }

    public Mono<ServerResponse> getRankingSteps(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);
        var imageRankingId = serverRequest.pathVariable("imageRankingId");
        return
                rankingStepService.getRankingStepsByImageRankingId(imageRankingId)
                        .collectList()
                        .flatMap(imageRankings -> ServerResponse.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(BodyInserters.fromValue(imageRankings)));
    }

    public Mono<ServerResponse> updateImageRankingRankingStep(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);
        var rankingStepMono = serverRequest.bodyToMono(RankingStep.class);
        return rankingStepMono.filter(step -> step.getUserId().equals(userId.id()))
                .flatMap(rankingStepService::updateRankingStep)
                .flatMap(imageRanking -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(imageRanking)));
    }

    public Mono<ServerResponse> updateImageRankingImages(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);
        var imageRankingId = serverRequest.pathVariable("imageRankingId");
        var imageRankingMono = imageRankingService.getImageRankingById(imageRankingId);
        return Mono.zip(imageRankingMono,
                        serverRequest.bodyToMono(UpdateImageRankingImagesRequest.class)
                                .map(UpdateImageRankingImagesRequest::images),
                        (imageRanking, imageList) -> Mono.just(imageRanking)
                                .filter(step -> step.getUserId().equals(userId.id()))
                                .map(ranking -> ImageRanking.builder()
                                        .id(ranking.getId())
                                        .imageIds(imageList)
                                        .build())
                                .flatMap(imageRankingService::updateImageRanking)
                                .flatMap(newRanking -> ServerResponse.ok()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(BodyInserters.fromValue(newRanking))))
                .flatMap(response -> response);
    }

    record UpdateImageRankingImagesRequest(List<String> images) {
    }

    public Mono<ServerResponse> deleteImageRanking(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest);
        var deletedRanking = serverRequest.bodyToMono(ImageRanking.class)
                .filter(step -> step.getUserId().equals(userId.id()))
                .flatMap(imageRankingService::deleteImageRanking).cache();
        return deletedRanking.map(ImageRanking::getId)
                .flatMapMany(rankingStepService::getRankingStepsByImageRankingId)
                .flatMap(rankingStepService::deleteRankingStep)
                .then(deletedRanking)
                .flatMap(imageRanking -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(imageRanking)));
    }

    public Mono<ServerResponse> uploadRankingState(final ServerRequest serverRequest) {
        return serverRequest.bodyToMono(UploadRankingStateRequest.class)
                .flatMap(request -> imageRankingService.uploadRankingState(request.imageRanking(), request.state()))
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(response)));
    }

    record UploadRankingStateRequest(ImageRanking imageRanking, String state) {

    }

    public Mono<ServerResponse> deleteRankingState(final ServerRequest serverRequest) {
        return serverRequest.bodyToMono(ImageRanking.class).flatMap(imageRankingService::deleteRankingState)
                .flatMap(response -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(response)));
    }
}
