package com.scryer.endpoint.service;

import com.scryer.model.ddb.ImageSrcModel;
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

import static com.scryer.util.ConsolidateUtil.getCombinedTags;

@Service
public class ImageService {
    private final DynamoDbTable<ImageSrcModel> imageSrcTable;


    public ImageService(final DynamoDbTable<ImageSrcModel> imageSrcTable) {
        this.imageSrcTable = imageSrcTable;
    }

    public Mono<ImageSrcModel> getImageSrc(final String imageSrcId) {
        return Mono.just(imageSrcTable.getItem(Key.builder().partitionValue(imageSrcId).build()));

    }

    public Flux<ImageSrcModel> getImageSrcForFolder(final String folderId) {
        var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(folderId).build());
        return Flux.fromStream(imageSrcTable.index("folder_index").query(QueryEnhancedRequest.builder()
                                                                                 .queryConditional(queryConditional)
                                                                                 .attributesToProject()
                                                                                 .build()).stream()
                                       .flatMap(page -> page.items().stream()))
                .parallel()
                .map(imageSrcTable::getItem)
                .sequential();

    }

    public Mono<ImageSrcModel> updateImageSrc(final ImageSrcModel imageSrc) {
        return Mono.just(imageSrcTable.updateItem(UpdateItemEnhancedRequest.builder(ImageSrcModel.class)
                                                          .item(imageSrc)
                                                          .ignoreNulls(true)
                                                          .build()));
    }

    public Mono<ImageSrcModel> addTagsToImageSrc(final ImageSrcModel imageSrc, List<String> tags) {
        return updateImageSrc(ImageSrcModel.builder()
                                      .id(imageSrc.getId())
                                      .lastModified(System.currentTimeMillis())
                                      .tags(getCombinedTags(imageSrc, tags))
                                      .build());
    }

    public Mono<ImageSrcModel> deleteImageSrcTags(final ImageSrcModel imageSrc, List<String> tags) {
        return updateImageSrc(ImageSrcModel.builder()
                                      .id(imageSrc.getId())
                                      .lastModified(System.currentTimeMillis())
                                      .tags(getCombinedTags(imageSrc, tags))
                                      .build());
    }

    public Mono<ImageSrcModel> addImageSrc(final ImageSrcModel imageSrc) {
        return Mono.justOrEmpty(imageSrcTable.putItemWithResponse(PutItemEnhancedRequest.builder(ImageSrcModel.class)
                                                                          .item(imageSrc)
                                                                          .build()).attributes())
                .switchIfEmpty(getImageSrc(imageSrc.getId()));
    }


    public Mono<ImageSrcModel> deleteImageSrc(final ImageSrcModel imageSrc) {
        return Mono.justOrEmpty(imageSrcTable.deleteItem(imageSrc));
    }

    public Mono<String> getUniqueId() {
        return Mono.just(IdGenerator.uniqueIdForTable(imageSrcTable, false));
    }
}

