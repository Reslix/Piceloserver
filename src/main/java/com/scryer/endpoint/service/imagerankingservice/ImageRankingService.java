package com.scryer.endpoint.service.imagerankingservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;

@Service
public class ImageRankingService {

	private final DynamoDbTable<ImageRankingModel> imageRankingTable;

	@Autowired
	public ImageRankingService(final DynamoDbTable<ImageRankingModel> imageRankingTable) {
		this.imageRankingTable = imageRankingTable;
	}

}
