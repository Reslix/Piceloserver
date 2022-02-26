package com.scryer.endpoint.service;

import com.scryer.model.ddb.ImageSrcModel;
import com.scryer.util.IdGenerator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class ImageService {
    private final DynamoDbTable<ImageSrcModel> imageSrcTable;
    private final S3Client s3Client;
    private final String s3BucketName;

    public ImageService(final DynamoDbTable<ImageSrcModel> imageSrcTable,
                        final S3Client s3Client,
                        final String s3BucketName) {
        this.imageSrcTable = imageSrcTable;
        this.s3Client = s3Client;
        this.s3BucketName = s3BucketName;
    }

    public Mono<ImageSrcModel> getImageSrcFromTable(final String imageSrcId) {
        return Mono.just(imageSrcTable.getItem(Key.builder().partitionValue(imageSrcId).build()));

    }

    public Flux<ImageSrcModel> getImageSrcForFolderFromTable(final String folderId) {
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

    public Mono<ImageSrcModel> updateImageSrcTable(final ImageSrcModel imageSrc) {
        return Mono.just(imageSrcTable.updateItem(UpdateItemEnhancedRequest.builder(ImageSrcModel.class)
                                                          .item(imageSrc)
                                                          .ignoreNulls(true)
                                                          .build()));
    }

    public Mono<ImageSrcModel> addToImageSrcTable(final ImageSrcModel imageSrc) {
        return Mono.justOrEmpty(imageSrcTable.putItemWithResponse(PutItemEnhancedRequest.builder(ImageSrcModel.class)
                                                                          .item(imageSrc)
                                                                          .build()).attributes())
                .switchIfEmpty(getImageSrcFromTable(imageSrc.getId()));
    }

    public Mono<String> addImagesToS3(final String id, final String size, final byte[] image, final String[] type) {
        var key = size + "/" + id + "." + type[2];
        var request = PutObjectRequest.builder()
                .bucket(s3BucketName)
                .key(key)
                .contentType(type[0]).build();
        var body = RequestBody.fromBytes(image);

        return Mono.justOrEmpty(s3Client.putObject(request, body))
                .then(Mono.just(GetUrlRequest.builder().bucket(s3BucketName).key(key).build()))
                .map(urlRequest -> s3Client.utilities().getUrl(urlRequest).toExternalForm());
    }

    public Mono<ImageSrcModel> deleteImageSrcFromTable(final ImageSrcModel imageSrc) {
        return Mono.justOrEmpty(imageSrcTable.deleteItem(imageSrc));
    }

    public Mono<String> getUniqueId() {
        return Mono.just(IdGenerator.uniqueIdForTable(imageSrcTable, false));
    }
}

