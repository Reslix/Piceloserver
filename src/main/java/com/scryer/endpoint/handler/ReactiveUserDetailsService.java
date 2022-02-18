package com.scryer.endpoint.handler;

import com.scryer.model.ddb.UserModel;
import com.scryer.model.ddb.UserSecurityModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.w3c.dom.Attr;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

@Service
public class ReactiveUserDetailsHandler implements ReactiveUserDetailsService {
    private final DynamoDbTable<UserSecurityModel> userSecurityTable;

    @Autowired
    public ReactiveUserDetailsHandler(final DynamoDbTable<UserSecurityModel> userSecurityTable) {
        this.userSecurityTable = userSecurityTable;
    }

    @Cacheable("userSecurity")
    @Override
    public Mono<UserDetails> findByUsername(final String username) {
        UserDetails userDetails = this.userSecurityTable.getItem(Key.builder().partitionValue(username).build());

        if (userDetails == null && username.contains("@")) {
            // In case it was an email that was submitted
            var emailAttribute = AttributeValue.builder().s(username).build();
            var expression = Expression.builder()
                                       .expression("email = :value")
                                       .putExpressionValue(":value", emailAttribute)
                                       .build();
            var queryEnhancedRequest = QueryEnhancedRequest.builder()
                                                           .filterExpression(expression)
                                                           .build();
            userDetails = this.userSecurityTable.query(queryEnhancedRequest)
                                                .items().iterator().next();
        }
        if (userDetails == null) {
            return Mono.error(new UsernameNotFoundException(username));
        } else {
            return Mono.just(userDetails);
        }
    }
}
