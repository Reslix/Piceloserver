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
    //Upload image
    //Delete images
    public Mono<ServerResponse> getImagesByFolder(final ServerRequest serverRequest) {
        String folderId = serverRequest.pathVariable("folderId");
        return imageService.getImageSrcForFolder(folderId).collectList()
                .flatMap(imageSrcs -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(imageSrcs)));
    }

    public Mono<ServerResponse> postImage(final ServerRequest serverRequest) {
        var usernameMono = Mono.justOrEmpty(jwtManager.getUserIdentity(serverRequest))
                .map(JWTManager.UserId::id);
        return usernameMono.flatMap(userId -> {
            var multivalueMono = serverRequest.multipartData().map(MultiValueMap::toSingleValueMap);
            var typeStringMono = multivalueMono.map(map -> map.get("type").content())
                    .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                            .map(buffer -> new String(buffer.asByteBuffer().array())))
                    .map(type -> {
                        var split = type.split("/");
                        return new String[] {type, split[0], split[1]};
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

            // convert size
            var thumbnailMono = fullImageMono
                    .zipWith(typeStringMono)
                    .flatMap(tuple -> imageResizeService.getThumbnailImage(tuple.getT1(), tuple.getT2()));

            // get the image IDs for both images - reserve their spots in the table.
            var thumbnailIdMono = imageService.getUniqueId()
                    .map(id -> ImageSrcModel.builder().id(id).build())
                    .flatMap(imageService::addImageSrc).map(ImageSrcModel::getId);

            // write images to s3
            var thumbnailLocation = Mono.zip(thumbnailIdMono, thumbnailMono, typeStringMono)
                    .map(t3 -> imageUploadService.uploadImage(t3.getT1(), "thumbnail", t3.getT2(), t3.getT3()))
                    .flatMap(response -> response)
                    .doOnError(error -> {
                        thumbnailIdMono.map(id -> ImageSrcModel.builder().id(id).build())
                                .map(imageService::deleteImageSrc);
                    });

            var fullImageLocation = Mono.zip(thumbnailIdMono, fullImageMono, typeStringMono)
                    .map(t3 -> imageUploadService.uploadImage(t3.getT1(), "full", t3.getT2(), t3.getT3()))
                    .flatMap(response -> response)
                    .doOnError(error -> {
                        thumbnailIdMono.map(id -> ImageSrcModel.builder().id(id).build())
                                .map(imageService::deleteImageSrc);
                    });

            // get imageSrc for both
            var thumbnailSrcMono = Mono.zip(thumbnailIdMono,
                                            nameStringMono,
                                            typeStringMono,
                                            thumbnailLocation,
                                            folderIdMono,
                                            fullImageLocation)
                    .flatMap(t6 -> createImageSrc(t6.getT1(),
                                                  t6.getT2(),
                                                  t6.getT3(),
                                                  "thumbnail",
                                                  t6.getT4(),
                                                  t6.getT5(),
                                                  userId,
                                                  "full",
                                                  t6.getT6()))
                    // For whatever reason method reference does not work
                    .flatMap(imageSrc -> imageService.updateImageSrc(imageSrc));

            // return thumbnail imageSrc
            // fullImageSrcMono.then(..) leads to fullImageSrcMono not running so I probably don't understand it at all
            return thumbnailSrcMono.flatMap(imageSrc -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(imageSrc)))
                    .switchIfEmpty(ServerResponse.status(HttpStatus.BAD_REQUEST).build());

        }).switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED).build());
    }

    public Mono<ServerResponse> deleteImage(final ServerRequest serverRequest) {
        return Mono.empty();
    }

    private Mono<ImageSrcModel> createImageSrc(final String imageId,
                                               final String name,
                                               final String[] type,
                                               final String size,
                                               final String path,
                                               final String folderId,
                                               final String userId,
                                               final String alternateSize,
                                               final String alternateSource) {
        long currentTime = System.currentTimeMillis();
        return Mono.just(ImageSrcModel.builder()
                                 .id(imageId)
                                 .userId(userId)
                                 .type(type[0])
                                 .name(name)
                                 .size(size)
                                 .source(new BaseIdentifier("url", path))
                                 .createDate(currentTime)
                                 .lastModified(currentTime)
                                 .parentFolderId(folderId)
                                 .alternateSizes(Map.of(alternateSize, new BaseIdentifier("url", alternateSource)))
                                 .imageRankings(List.of())
                                 .build());

    }
}
