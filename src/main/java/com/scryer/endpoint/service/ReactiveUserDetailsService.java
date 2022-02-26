package com.scryer.endpoint.service;

import com.scryer.model.ddb.UserSecurityModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.util.List;

@Service
public class ReactiveUserDetailsService implements org.springframework.security.core.userdetails.ReactiveUserDetailsService {

    private final DynamoDbTable<UserSecurityModel> userSecurityTable;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public ReactiveUserDetailsService(final DynamoDbTable<UserSecurityModel> userSecurityTable) {
        this.userSecurityTable = userSecurityTable;
        this.passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Cacheable("userSecurity")
    @Override
    public Mono<UserDetails> findByUsername(final String username) {
        return getUserFromTable(username).switchIfEmpty(Mono.just(username).filter(name -> name.contains("@")).
                                                                flatMap(this::getUserByEmailFromTable))
                .cast(UserDetails.class)
                .switchIfEmpty(Mono.error(new UsernameNotFoundException(username)));
    }

    public Mono<UserSecurityModel> getUserFromTable(final String username) {
        return Mono.justOrEmpty(this.userSecurityTable.getItem(Key.builder().partitionValue(username).build()));
    }

    public Mono<UserSecurityModel> getUserByEmailFromTable(final String email) {
        var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build());
        var queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .attributesToProject()
                .build();
        return Mono.justOrEmpty(this.userSecurityTable.index("email_index")
                                        .query(queryEnhancedRequest)
                                        .stream()
                                        .flatMap(page -> page.items().stream())
                                        .findFirst());

    }

    public Mono<UserSecurityModel> addUserSecurityToTable(final UserService.NewUserRequest request, final String id) {
        var userSecurityModel = UserSecurityModel.builder()
                .username(request.username())
                .email(request.email())
                .id(id)
                .password(passwordEncoder.encode(request.password()))
                .enabled(true)
                .credentialsNonExpired(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .authorities(List.of(new SimpleGrantedAuthority("user")))
                .build();
        var enhancedRequest = PutItemEnhancedRequest.builder(UserSecurityModel.class).item(userSecurityModel).build();
        return Mono.fromCallable(() -> {
            userSecurityTable.putItemWithResponse(enhancedRequest);
            return userSecurityTable.getItem(Key.builder().partitionValue(request.username()).build());
        });
    }
}
