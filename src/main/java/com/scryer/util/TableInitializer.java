package com.scryer.util;

import com.scryer.model.ddb.DynamoDBTableModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;

import java.util.List;
import java.util.Map;

/**
 * Creates tables if they have not already been created
 */
@Component
public class TableInitializer {

    public static <T extends DynamoDBTableModel> DynamoDbTable<T> getOrCreateTable(final DynamoDbEnhancedClient client,
                                                                                   final Class<T> tableClass) {
        return client.table(tableClass.getName(), TableSchema.fromImmutableClass(tableClass));
    }
}