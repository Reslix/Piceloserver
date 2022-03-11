package com.scryer.util;

import com.scryer.endpoint.service.DynamoDBTableModel;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.CreateTableEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

/**
 * Creates tables if they have not already been created
 */
@Component
public final class TableInitializer {

	public static <T extends DynamoDBTableModel> DynamoDbTable<T> getOrCreateTable(final DynamoDbEnhancedClient client,
			final Class<T> tableClass, final CreateTableEnhancedRequest request) {
		DynamoDbTable<T> table = client.table(tableClass.getSimpleName(), TableSchema.fromImmutableClass(tableClass));
		try {
			table.describeTable();
		}
		catch (ResourceNotFoundException e) {
			table.createTable(request);
		}
		return table;
	}

}