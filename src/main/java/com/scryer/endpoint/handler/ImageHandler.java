package com.scryer.endpoint.handler;

import com.scryer.endpoint.security.JWTManager;
import com.scryer.endpoint.service.ImageResizeService;
import com.scryer.endpoint.service.ImageService;
import com.scryer.endpoint.service.ImageUploadService;
import com.scryer.model.ddb.BaseIdentifier;
import com.scryer.model.ddb.ImageSrcModel;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class ImageHandler {

    private final ImageService imageService;
    private final ImageUploadService imageUploadService;
    private final ImageResizeService imageResizeService;
    private final JWTManager jwtManager;

    public ImageHandler(final ImageService imageService,
                        final ImageUploadService imageUploadService,
                        final ImageResizeService imageResizeService,
                        final JWTManager jwtManager) {
        this.imageService = imageService;
        this.imageUploadService = imageUploadService;
        this.imageResizeService = imageResizeService;
        this.jwtManager = jwtManager;
    }

    //Get imagesrc by folder
    //Upload imageIds
    //Delete images
    public Mono<ServerResponse> getImagesByFolder(final ServerRequest serverRequest) {
        String folderId = serverRequest.pathVariable("folderId");
        return imageService.getImageSrcForFolder(folderId).collectList()
                .flatMap(imageSrcs -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(imageSrcs)));
    }

    public Mono<ServerResponse> postImage(final ServerRequest serverRequest) {
        var userIdMono = Mono.justOrEmpty(jwtManager.getUserIdentity(serverRequest))
                .map(JWTManager.UserIdentity::id);
        return userIdMono.flatMap(userId -> {
            var multivalueMono = serverRequest.multipartData().map(MultiValueMap::toSingleValueMap);
            var typeStringMono = multivalueMono.map(map -> map.get("type").content())
                    .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                            .map(buffer -> new String(buffer.asByteBuffer().array())))
                    .map(type -> {
                        var split = type.split("/");
                        return new String[]{type, split[0], split[1]};
                    });

            var nameStringMono = multivalueMono.map(map -> map.get("name").content())
                    .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                            .map(buffer -> new String(buffer.asByteBuffer().array())));

            var folderIdMono = multivalueMono.map(map -> map.get("folderId").content())
                    .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                            .map(buffer -> new String(buffer.asByteBuffer().array())));

            var fullImageMono = multivalueMono.map(map -> map.get("image").content())
                    .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                            .map(buffer -> buffer.asByteBuffer().array()));


            return Mono.zip(nameStringMono, typeStringMono, folderIdMono, fullImageMono)
                    .flatMap(t4 -> imageService.addImage(t4.getT1(), t4.getT2(), t4.getT3(), t4.getT4(), userId)
                            .flatMap(imageSrc -> ServerResponse.ok()
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .body(BodyInserters.fromValue(imageSrc)))
                            .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build()));

        }).switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED).build());
    }

    public Mono<ServerResponse> moveImages(final ServerRequest serverRequest) {
        String folderId = serverRequest.pathVariable("folderId");
        var imageSrcModelFlux = serverRequest.bodyToFlux(ImageSrcModel.class).cache();
        return imageSrcModelFlux.map(image -> ImageSrcModel.builder().id(image.getId()).parentFolderId(folderId).build())
                .flatMap(imageService::updateImageSrc)
                .collectList()
                .flatMap(images -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(images)))
                .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build());
    }


    public Mono<ServerResponse> deleteImages(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
        return serverRequest.bodyToFlux(ImageSrcModel.class)
                .filter(imageSrcModel -> imageSrcModel.getUserId().equals(userId))
                .flatMap(imageService::deleteImage)
                .collectList()
                .flatMap(image -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(image)))
                .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build());
    }

    public Mono<ServerResponse> deleteImage(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
        return serverRequest.bodyToMono(ImageSrcModel.class)
                .filter(imageSrcModel -> imageSrcModel.getUserId().equals(userId))
                .flatMap(imageService::deleteImage)
                .flatMap(image -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(image)))
                .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build());
    }
}
