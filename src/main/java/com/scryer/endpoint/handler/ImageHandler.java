package com.scryer.endpoint.handler;

import com.scryer.endpoint.security.JWTManager;
import com.scryer.endpoint.service.imageresize.ImageResizeRequest;
import com.scryer.endpoint.service.imageresize.ImageResizeService;
import com.scryer.endpoint.service.imagesrc.ImageBaseIdentifier;
import com.scryer.endpoint.service.imagesrc.ImageService;
import com.scryer.endpoint.service.imagesrc.ImageSrc;
import com.scryer.endpoint.service.imageupload.ImageUploadRequest;
import com.scryer.endpoint.service.imageupload.ImageUploadService;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

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

    // Get imagesrc by folder
    // Upload imageIds
    // Delete images
    public Mono<ServerResponse> getImagesByFolder(final ServerRequest serverRequest) {
        String folderId = serverRequest.pathVariable("folderId");
        return imageService.getImageSrcForFolder(folderId).collectList().flatMap(imageSrcs -> ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON).body(BodyInserters.fromValue(imageSrcs)));
    }

    public Mono<ServerResponse> postImage(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
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

        var lastModifiedMono = multivalueMono.map(map -> map.get("lastModified").content())
                .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                        .map(buffer -> new String(buffer.asByteBuffer().array()))).map(Long::parseLong);

        var fullImageMono = multivalueMono.map(map -> map.get("image").content()).flatMap(
                bufferFlux -> DataBufferUtils.join(bufferFlux).map(buffer -> buffer.asByteBuffer().array()));

        return Mono.zip(nameStringMono, typeStringMono, folderIdMono, fullImageMono, lastModifiedMono)
                .flatMap(t5 -> {
                    var name = t5.getT1();
                    var type = t5.getT2();
                    var folderId = t5.getT3();
                    var image = t5.getT4();
                    var lastModified = t5.getT5();
                    var thumbnailMono = imageResizeService
                            .getResizedImage(new ImageResizeRequest(image, type, 300));
                    var mediumMono = imageResizeService
                            .getResizedImage(new ImageResizeRequest(image, type, 1920));
                    var uniqueIdMono = imageService.getUniqueId().cache();
                    var thumbnailLocation = Mono.zip(uniqueIdMono, thumbnailMono)
                            .flatMap(t2 -> imageUploadService
                                    .uploadImage(new ImageUploadRequest(t2.getT1(), "thumbnail", t2.getT2(), type)))
                            .doOnError(error -> uniqueIdMono.map(id -> ImageSrc.builder().id(id).build())
                                    .map(imageService::deleteImage));

                    var mediumLocation = Mono.zip(uniqueIdMono, mediumMono)
                            .flatMap(t2 -> imageUploadService
                                    .uploadImage(new ImageUploadRequest(t2.getT1(), "medium", t2.getT2(), type)))
                            .doOnError(error -> uniqueIdMono.map(id -> ImageSrc.builder().id(id).build())
                                    .map(imageService::deleteImage));

                    var fullImageLocation = uniqueIdMono
                            .flatMap(id -> imageUploadService.uploadImage(new ImageUploadRequest(id,
                                                                                                 "full",
                                                                                                 image,
                                                                                                 type)))
                            .doOnError(error -> uniqueIdMono.map(id -> ImageSrc.builder().id(id).build())
                                    .map(imageService::deleteImage));

                    return Mono.zip(uniqueIdMono,
                                    thumbnailLocation,
                                    mediumLocation,
                                    fullImageLocation)
                            .flatMap(t4_2 -> {
                                return imageService
                                        .addImageSrc(ImageSrc.builder()
                                                             .id(t4_2.getT1())
                                                             .userId(userId)
                                                             .type(type[0])
                                                             .name(name)
                                                             .size("thumbnail")
                                                             .source(new ImageBaseIdentifier("url", t4_2.getT2()))
                                                             .createDate(lastModified)
                                                             .lastModified(lastModified)
                                                             .parentFolderId(folderId)
                                                             .alternateSizes(
                                                                     Map.of("medium",
                                                                            new ImageBaseIdentifier("url",
                                                                                                    t4_2.getT3()),
                                                                            "full",
                                                                            new ImageBaseIdentifier("url",
                                                                                                    t4_2.getT4())))
                                                             .build());
                            });
                })
                .flatMap(imageSrc -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(imageSrc)))
                .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).
                                       build());
    }

    public Mono<ServerResponse> moveImages(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
        var folderId = serverRequest.pathVariable("folderId");
        var imageSrcModelFlux = serverRequest.bodyToFlux(ImageSrc.class).cache();
        return imageSrcModelFlux.filter(image -> image.getUserId().equals(userId))
                .map(image -> ImageSrc.builder().id(image.getId()).parentFolderId(folderId).build())
                .flatMap(imageService::updateImageSrc)
                .collectList()
                .flatMap(images -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(images)))
                .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build());
    }

    public Mono<ServerResponse> deleteImages(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
        var images = serverRequest.bodyToFlux(ImageSrc.class)
                .filter(imageSrcModel -> imageSrcModel.getUserId().equals(userId))
                .cache();
        var deletedImages = images.flatMap(image -> imageUploadService.deleteImage(image.getSource().src())
                .thenReturn(image)
                .flatMapIterable(i -> i.getAlternateSizes().entrySet())
                .map(entry -> imageUploadService.deleteImage(entry.getValue().src()))
                .then(imageService.deleteImage(image)));
        return deletedImages.collectList()
                .flatMap(imageList -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(imageList)))
                .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build());
    }

    public Mono<ServerResponse> deleteImage(final ServerRequest serverRequest) {
        var userId = jwtManager.getUserIdentity(serverRequest).id();
        return serverRequest.bodyToMono(ImageSrc.class)
                .filter(imageSrcModel -> imageSrcModel.getUserId().equals(userId))
                .flatMap(image -> imageUploadService.deleteImage(image.getSource().src())
                        .thenReturn(image)
                        .flatMapIterable(i -> i.getAlternateSizes().entrySet())
                        .map(entry -> imageUploadService.deleteImage(entry.getValue().src()))
                        .then(imageService.deleteImage(image)))
                .flatMap(image -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(image)))
                .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build());
    }
}
