package com.picelo.endpoint.service.imagerankings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
class ImageRankingS3ObjectAdapter implements ImageRankingObjectAdapter{
    private final S3Client s3Client;
    private final String s3RankingBucketName;

    @Autowired
    public ImageRankingS3ObjectAdapter(final S3Client s3Client, final String s3RankingBucketName) {
        this.s3Client = s3Client;
        this.s3RankingBucketName = s3RankingBucketName;
    }

    @Override
    public Mono<String> uploadRankingState(final String imageRankingId, final String state) {
        var key = "rankingstate" + "/" + imageRankingId;
        return Mono.just(s3Client.putObject(PutObjectRequest.builder().bucket(s3RankingBucketName).key(key).build(),
                                            RequestBody.fromString(state)))
                .then(Mono.just(GetUrlRequest.builder().bucket(s3RankingBucketName).key(key).build()))
                .map(urlRequest -> s3Client.utilities().getUrl(urlRequest).toExternalForm());
    }

    @Override
    public Mono<String> deleteImage(final String path) {
        var splitPath = path.split("/");
        var key = splitPath[splitPath.length - 2] + "/" + splitPath[splitPath.length - 1];
        return Mono.just(s3Client.deleteObject(DeleteObjectRequest.builder().bucket(s3RankingBucketName).key(key).build())
                                 .toString());
    }
}
