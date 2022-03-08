package com.scryer.endpoint.service;

import com.scryer.model.ddb.ImageSrcModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class ImageUploadService {
    private final S3Client s3Client;
    private final String s3BucketName;

    @Autowired
    public ImageUploadService(final S3Client s3Client,
                              final String s3BucketName) {
        this.s3Client = s3Client;
        this.s3BucketName = s3BucketName;
    }

    public Mono<String> uploadImage(final String id, final String size, final byte[] image, final String[] type) {
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

    public Mono<String> deleteImage(final String path) {
        var splitPath = path.split("/");
        var key = splitPath[splitPath.length-2] + "/" + splitPath[splitPath.length-1];
        return Mono.just(s3Client.deleteObject(DeleteObjectRequest.builder().bucket(s3BucketName).key(key).build()).toString());
    }
}
