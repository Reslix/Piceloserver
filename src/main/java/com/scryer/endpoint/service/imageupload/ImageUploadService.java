package com.scryer.endpoint.service.imageupload;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
public class ImageUploadService {

    private final S3Client s3Client;

    private final String s3BucketName;

    @Autowired
    public ImageUploadService(final S3Client s3Client, final String s3BucketName) {
        this.s3Client = s3Client;
        this.s3BucketName = s3BucketName;
    }

    public Mono<String> uploadImage(final ImageUploadRequest request) {
        var key = request.size() + "/" + request.id() + "." + request.type()[2];
        var s3Request = PutObjectRequest.builder().bucket(s3BucketName).key(key).contentType(request.type()[0]).build();
        var body = RequestBody.fromBytes(request.image());

        return Mono.justOrEmpty(s3Client.putObject(s3Request, body))
                .then(Mono.just(GetUrlRequest.builder().bucket(s3BucketName).key(key).build()))
                .map(urlRequest -> s3Client.utilities().getUrl(urlRequest).toExternalForm());
    }

    public Mono<String> deleteImage(final String path) {
        var splitPath = path.split("/");
        var key = splitPath[splitPath.length - 2] + "/" + splitPath[splitPath.length - 1];
        return Mono.just(
                s3Client.deleteObject(DeleteObjectRequest.builder().bucket(s3BucketName).key(key).build()).toString());
    }

}
