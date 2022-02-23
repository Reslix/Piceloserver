package com.scryer.endpoint.handler;

import com.scryer.endpoint.service.FolderService;
import com.scryer.endpoint.service.ImageSrcService;
import com.scryer.model.ddb.BaseIdentifier;
import com.scryer.model.ddb.FolderModel;
import com.scryer.model.ddb.ImageSrcModel;
import com.scryer.endpoint.security.JWTManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class ImageSrcHandler {

    private final ImageSrcService imageSrcService;
    private final FolderService folderService;
    private final JWTManager jwtManager;

    public ImageSrcHandler(final ImageSrcService imageSrcService,
                           final FolderService folderService,
                           final JWTManager jwtManager) {
        this.imageSrcService = imageSrcService;
        this.folderService = folderService;
        this.jwtManager = jwtManager;
    }

    //Get imagesrc by folder
    //Upload image
    //Delete images
    public Mono<ServerResponse> getImagesByFolder(final ServerRequest request) {
        String folderId = request.pathVariable("folderId");
        return imageSrcService.getImageSrcForFolderFromTable(folderId).collectList()
                .flatMap(imageSrcs -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(imageSrcs)));
    }

    public Mono<ServerResponse> postImage(final ServerRequest request) {
        var usernameMono = Mono.justOrEmpty(jwtManager.getUserIdentity(request))
                .map(JWTManager.UserId::id);
        return usernameMono.flatMap(userId -> {
            var multivalueMono = request.multipartData().map(MultiValueMap::toSingleValueMap);
            var typeStringMono = multivalueMono.map(map -> map.get("type").content())
                    .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                            .map(buffer -> new String(buffer.asByteBuffer().array())));

            var nameStringMono = multivalueMono.map(map -> map.get("name").content())
                    .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                            .map(buffer -> new String(buffer.asByteBuffer().array())));

            var folderIdMono = multivalueMono.map(map -> map.get("folderId").content())
                    .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                            .map(buffer -> new String(buffer.asByteBuffer().array())));

            var imageMono = multivalueMono.map(map -> map.get("image").content())
                    .flatMap(bufferFlux -> DataBufferUtils.join(bufferFlux)
                            .map(buffer -> buffer.asByteBuffer().array()));

            var fullImageSrcMono = Mono.zip(nameStringMono, imageMono, (name, image) -> writeFullImage(image, name));

            var thumbnailImageSrcMono =
                    Mono.zip(Mono.zip(nameStringMono, imageMono, typeStringMono)
                                     .map(tuple -> writeThumbnailImage(tuple.getT2(), tuple.getT1(), tuple.getT3())),
                             nameStringMono,
                             typeStringMono,
                             folderIdMono,
                             imageSrcService.getUniqueId(),
                             fullImageSrcMono)
                            .flatMap(tuple -> createThumbnailImageSrc(tuple.getT1(),
                                                                      tuple.getT2(),
                                                                      tuple.getT3(),
                                                                      tuple.getT4(),
                                                                      tuple.getT5(),
                                                                      userId,
                                                                      tuple.getT6()));


            return thumbnailImageSrcMono.flatMap(imageSrc1 -> imageSrcService.updateImageSrcTable(imageSrc1))
                    .flatMap(imageSrc -> ServerResponse.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(BodyInserters.fromValue(imageSrc)));
        }).switchIfEmpty(ServerResponse.status(HttpStatus.UNAUTHORIZED).build());
    }

    public Mono<ServerResponse> deleteImage(final ServerRequest request) {
        return Mono.empty();
    }

    private Mono<ImageSrcModel> createThumbnailImageSrc(final String path,
                                                        final String name,
                                                        final String type,
                                                        final String folderId,
                                                        final String imageId,
                                                        final String userId,
                                                        final String fullImagePath) {
        long currentTime = System.currentTimeMillis();
        return Mono.just(ImageSrcModel.builder()
                                 .id(imageId)
                                 .userId(userId)
                                 .type(type)
                                 .name(name)
                                 .size("thumbnail")
                                 .source(new BaseIdentifier("static", path))
                                 .createDate(currentTime)
                                 .lastModified(currentTime)
                                 .parentFolderId(folderId)
                                 .alternateSizes(Map.of("full", new BaseIdentifier("static", fullImagePath)))
                                 .build());

    }

    private String writeThumbnailImage(final byte[] image, final String filename, final String type) {
        try {
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(image));
            var height = original.getHeight();
            var width = original.getWidth();
            double ratio;
            BufferedImage resized;
            if (width < height) {
                ratio = 400.0 / height;
                var targetWidth = Double.valueOf(width * ratio).intValue();
                resized = new BufferedImage(targetWidth, 400, original.getType());
                Graphics2D graphics2D = resized.createGraphics();
                graphics2D.drawImage(original, 0, 0, targetWidth, 400, null);
                graphics2D.dispose();
            } else {
                ratio = 400.0 / width;
                var targetHeight = Double.valueOf(height * ratio).intValue();
                resized = new BufferedImage(400, targetHeight, original.getType());
                Graphics2D graphics2D = resized.createGraphics();
                graphics2D.drawImage(original, 0, 0, 400, targetHeight, null);
                graphics2D.dispose();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(resized, type.split("/")[1], out);
            Files.write(Path.of("build/classes/java/main/static/thumbnail/" + filename), out.toByteArray());
            return "thumbnail/" + filename;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String writeFullImage(final byte[] image, final String filename) {
        try {
            Files.write(Path.of("build/classes/java/main/static/full/" + filename), image);
            return "full/" + filename;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
