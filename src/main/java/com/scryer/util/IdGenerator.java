package com.scryer.util;

import com.scryer.model.ddb.HasId;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.concurrent.ThreadLocalRandom;

public class IdGenerator {
    /**
     * AKA generate a random long
     *
     * @return
     */
    public static long uniqueIdForTable(final DynamoDbTable<? extends HasId> table, final Boolean isPrimaryKey) {
        boolean found = true;
        for (int attempts = 0; attempts < 5; attempts++) {
            Long candidate = ThreadLocalRandom.current().nextLong();
            if (isPrimaryKey) {
                found = table.getItem(Key.builder().partitionValue(candidate).build()) != null;
            } else {
                var idAttribute = AttributeValue.builder().n(candidate.toString()).build();
                var expression = Expression.builder()
                                           .expression("id = :value")
                                           .putExpressionValue(":value", idAttribute)
                                           .build();
                var queryEnhancedRequest = QueryEnhancedRequest.builder()
                                                               .filterExpression(expression)
                                                               .build();
                found = !table.query(queryEnhancedRequest).items().stream().findAny().isEmpty();
            }
            if (!found) {
                return candidate;
            }
        }
        throw new RuntimeException("Could not find unique ID");
    }
}
