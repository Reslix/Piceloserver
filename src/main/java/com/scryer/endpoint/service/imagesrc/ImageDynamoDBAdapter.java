package com.scryer.endpoint.service.imagesrc;

import com.scryer.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.Key;

@Service
class ImageDynamoDBAdapter implements ImageDBAdapter {
    private final DynamoDbTable<ImageSrc> imageSrcTable;
    private final ReactiveRedisTemplate<String, ImageSrc> imageRedisTemplate;

    @Autowired
    ImageDynamoDBAdapter(final DynamoDbTable<ImageSrc> imageSrcTable,
                         final ReactiveRedisTemplate<String, ImageSrc> imageRedisTemplate) {
        this.imageSrcTable = imageSrcTable;
        this.imageRedisTemplate = imageRedisTemplate;
    }

    @Override
    public Mono<ImageSrc> getImageSrc(final String imageSrcId) {
        return imageRedisTemplate.opsForHash()
                .get(imageSrcId, imageSrcId)
                .cast(ImageSrc.class)
                .switchIfEmpty(Mono.defer(() -> {
                    var imageSrc = imageSrcTable.getItem(software.amazon.awssdk.enhanced.dynamodb.Key.builder()
                                                                 .partitionValue(imageSrcId)
                                                                 .build());
                    return imageRedisTemplate.opsForHash().put(imageSrcId, imageSrcId, imageSrc).thenReturn(imageSrc);
                }));
    }

    @Override
    public Flux<ImageSrc> getImageSrcForFolder(final String folderId) {
        var queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(folderId).build());
        return Flux.fromStream(imageSrcTable.index("folder_index")
                                       .query(QueryEnhancedRequest.builder()
                                                      .queryConditional(queryConditional)
                                                      .attributesToProject()
                                                      .build())
                                       .stream().flatMap(page -> page.items().stream()))
                .parallel().flatMap(image -> getImageSrc(image.getId())).sequential();
    }

    @Override
    public Mono<ImageSrc> updateImageSrc(final ImageSrc imageSrc) {
        var newImage = Mono.just(imageSrcTable.updateItem(UpdateItemEnhancedRequest.builder(ImageSrc.class)
                                                                  .item(imageSrc)
                                                                  .ignoreNulls(true)
                                                                  .build())).cache();
        return newImage.map(image -> imageRedisTemplate.opsForHash().put(image.getId(), image.getId(), image))
                .then(newImage);

    }

    @Override
    public Mono<ImageSrc> addImageSrc(final ImageSrc imageSrc) {
        return Mono.justOrEmpty(imageSrcTable.putItemWithResponse(PutItemEnhancedRequest.builder(ImageSrc.class)
                                                                          .item(imageSrc)
                                                                          .build()).attributes())
                .switchIfEmpty(getImageSrc(imageSrc.getId()));
    }

    @Override
    public Mono<ImageSrc> deleteImage(final ImageSrc imageSrc) {
        return imageRedisTemplate.opsForHash()
                .delete(imageSrc.getId())
                .thenReturn(imageSrc)
                .map(imageSrcTable::deleteItem);
    }

    @Override
    public Mono<String> getUniqueId() {
        return Mono.just(IdGenerator.uniqueIdForTable(imageSrcTable, false));
    }
}
