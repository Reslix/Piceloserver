package com.scryer.util;

import com.scryer.endpoint.service.HasId;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbIndex;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.concurrent.ThreadLocalRandom;

public final class IdGenerator {

	/**
	 * AKA generate a random long
	 * @return
	 */
	public static String uniqueIdForTable(final DynamoDbTable<? extends HasId> table, final Boolean isPrimaryKey) {
		boolean found = true;
		for (int attempts = 0; attempts < 5; attempts++) {
			String candidate = randomId();
			if (isPrimaryKey) {
				found = table.getItem(Key.builder().partitionValue(candidate).build()) != null;
			}
			else {
				var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(candidate).build());
				var queryEnhancedRequest = QueryEnhancedRequest.builder().queryConditional(queryConditional)
						.consistentRead(true).build();
				found = !table.query(queryEnhancedRequest).items().stream().toList().isEmpty();
			}
			if (!found) {
				return candidate;
			}
		}
		throw new RuntimeException("Could not find unique ID");
	}

	public static String uniqueIdForIndex(final DynamoDbIndex<? extends HasId> index, final Boolean isPrimaryKey) {
		boolean found = true;
		for (int attempts = 0; attempts < 5; attempts++) {
			String candidate = randomId();

			var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(candidate).build());
			var queryEnhancedRequest = QueryEnhancedRequest.builder().queryConditional(queryConditional).build();
			found = !index.query(queryEnhancedRequest).stream().filter(page -> !page.items().isEmpty()).toList()
					.isEmpty();
			if (!found) {
				return candidate;
			}
		}
		throw new RuntimeException("Could not find unique ID");
	}

	private static String randomId() {
		String symbols = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		StringBuilder str = new StringBuilder();
		for (int i = 0; i < 16; i++) {
			str.append(symbols.charAt(ThreadLocalRandom.current().nextInt(symbols.length())));
		}
		return str.toString();

	}
}