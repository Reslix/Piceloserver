package com.scryer.endpoint.service.imagesrc;

import com.scryer.util.IdGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.List;
import java.util.Map;

import static com.scryer.util.ConsolidateUtil.getCombined;
import static com.scryer.util.ConsolidateUtil.getSubtracted;

@Service
public class ImageService {

    private final DynamoDbTable<ImageSrcModel> imageSrcTable;

    private final ImageUploader imageUploader;

    private final ImageResizer imageResizer;

    public ImageService(final DynamoDbTable<ImageSrcModel> imageSrcTable, final ImageUploader imageUploader,
                        final ImageResizer imageResizer) {
        this.imageSrcTable = imageSrcTable;
        this.imageUploader = imageUploader;
        this.imageResizer = imageResizer;
    }

    public Mono<ImageSrcModel> getImageSrc(final String imageSrcId) {
        return Mono.just(imageSrcTable.getItem(Key.builder().partitionValue(imageSrcId).build()));

    }

    public Flux<ImageSrcModel> getImageSrcForFolder(final String folderId) {
        var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(folderId).build());
        return Flux
                .fromStream(imageSrcTable.index("folder_index")
                                    .query(QueryEnhancedRequest.builder()
                                                   .queryConditional(queryConditional)
                                                   .attributesToProject()
                                                   .build())
                                    .stream().flatMap(page -> page.items().stream()))
                .parallel().map(imageSrcTable::getItem).sequential();

    }

    public Mono<ImageSrcModel> updateImageSrc(final ImageSrcModel imageSrc) {
        return Mono.just(imageSrcTable.updateItem(
                UpdateItemEnhancedRequest.builder(ImageSrcModel.class).item(imageSrc).ignoreNulls(true).build()));
    }

    public Mono<ImageSrcModel> addTagsToImageSrc(final ImageSrcModel imageSrc, List<String> tags) {
        return updateImageSrc(ImageSrcModel.builder()
                                      .id(imageSrc.getId())
                                      .lastModified(System.currentTimeMillis())
                                      .tags(getCombined(imageSrc.getTags(), tags))
                                      .build());
    }

    public Mono<ImageSrcModel> addImageRankingToImageSrc(final ImageSrcModel imageSrc, final String ranking) {
        return updateImageSrc(ImageSrcModel.builder()
                                      .id(imageSrc.getId())
                                      .lastModified(System.currentTimeMillis())
                                      .imageRankings(getCombined(imageSrc.getImageRankings(), List.of(ranking)))
                                      .build());
    }

    public Mono<ImageSrcModel> deleteImageSrcTags(final ImageSrcModel imageSrc, List<String> tags) {
        return updateImageSrc(ImageSrcModel.builder()
                                      .id(imageSrc.getId())
                                      .lastModified(System.currentTimeMillis())
                                      .tags(getSubtracted(imageSrc.getTags(), tags))
                                      .build());
    }

    public Mono<ImageSrcModel> deleteImageSrcImageRanking(final ImageSrcModel imageSrc, final String ranking) {
        return updateImageSrc(ImageSrcModel.builder()
                                      .id(imageSrc.getId())
                                      .lastModified(System.currentTimeMillis())
                                      .imageRankings(getSubtracted(imageSrc.getImageRankings(), List.of(ranking)))
                                      .build());
    }

    public Mono<ImageSrcModel> addImage(final String name, final String[] type, final String folderId,
                                        final byte[] image, final String userId) {
        // convert size
        var thumbnailMono = imageResizer.getThumbnailImage(image, type);

        // get the imageIds IDs for both images - reserve their spots in the table.
        var thumbnailIdMono = getUniqueId().map(id -> ImageSrcModel.builder().id(id).build()).flatMap(this::addImageSrc)
                .map(ImageSrcModel::getId);

        // write images to s3
        var thumbnailLocation = Mono.zip(thumbnailIdMono, thumbnailMono)
                .map(t2 -> imageUploader.uploadImage(t2.getT1(), "thumbnail", t2.getT2(), type))
                .flatMap(response -> response).doOnError(error -> {
                    thumbnailIdMono.map(id -> ImageSrcModel.builder().id(id).build()).map(this::deleteImage);
                });

        var fullImageLocation = thumbnailIdMono
                .map(thumbnail -> imageUploader.uploadImage(thumbnail, "full", image, type))
                .flatMap(response -> response).doOnError(error -> {
                    thumbnailIdMono.map(id -> ImageSrcModel.builder().id(id).build()).map(this::deleteImage);
                });

        return Mono.zip(thumbnailIdMono, thumbnailLocation, fullImageLocation, getUniqueId())
                .flatMap(t4 -> createImageSrc(t4.getT4(), name, type, "thumbnail", t4.getT2(), folderId, userId, "full",
                                              t4.getT3()))
                // For whatever reason method reference does not work
                .flatMap(this::updateImageSrc);

    }

    public Mono<ImageSrcModel> addImageSrc(final ImageSrcModel imageSrc) {
        return Mono.justOrEmpty(imageSrcTable
                                        .putItemWithResponse(PutItemEnhancedRequest.builder(ImageSrcModel.class)
                                                                     .item(imageSrc)
                                                                     .build())
                                        .attributes()).switchIfEmpty(getImageSrc(imageSrc.getId()));
    }

    public Mono<ImageSrcModel> deleteImage(final ImageSrcModel imageSrc) {
        return Mono.just(imageSrc).map(image -> {
            imageUploader.deleteImage(image.getSource().src());
            image.getAlternateSizes().forEach((key, value) -> imageUploader.deleteImage(value.src()));
            return imageSrcTable.deleteItem(image);
        });
    }

    public Mono<String> getUniqueId() {
        return Mono.just(IdGenerator.uniqueIdForTable(imageSrcTable, false));
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
                                 .source(new ImageBaseIdentifier("url", path))
                                 .createDate(currentTime)
                                 .lastModified(currentTime)
                                 .parentFolderId(folderId)
                                 .alternateSizes(Map.of(alternateSize, new ImageBaseIdentifier("url", alternateSource)))
                                 .imageRankings(List.of())
                                 .tags(List.of())
                                 .build());

    }

}
