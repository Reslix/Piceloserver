package com.scryer.endpoint.service;

import com.scryer.model.ddb.ImageRankingsModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

@Service
public class ImageRankingsService {
    private final DynamoDbTable<ImageRankingsModel> imageRankingsTable;

    @Autowired
    public ImageRankingsService(final DynamoDbTable<ImageRankingsModel> imageRankingsTable) {
        this.imageRankingsTable = imageRankingsTable;
    }
}
