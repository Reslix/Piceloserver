package com.scryer.endpoint.service;

import com.scryer.model.ddb.UserModel;
import com.scryer.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ReturnValue;

import java.util.List;

@Service
public class UserService {

    private final DynamoDbTable<UserModel> userTable;

    @Autowired
    public UserService(final DynamoDbTable<UserModel> userTable) {
        this.userTable = userTable;
    }

    public Mono<UserModel> getUserByIdFromTable(final String id) {
        var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(id).build());
        var queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .attributesToProject()
                .build();
        return Mono.justOrEmpty(this.userTable.index("userId_index")
                                        .query(queryEnhancedRequest)
                                        .stream()
                                        .flatMap(page -> page.items().stream()).findFirst());

    }

    public Mono<UserModel> getUserByUsernameFromTable(final String username) {
        return Mono.fromCallable(() -> userTable.getItem(Key.builder().partitionValue(username).build()));
    }

    public Mono<UserModel> getUserByEmailFromTable(final String email) {
        var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build());
        var queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .attributesToProject()
                .build();
        return Mono.justOrEmpty(this.userTable.index("email_index")
                                        .query(queryEnhancedRequest)
                                        .stream()
                                        .flatMap(page -> page.items().stream()).findFirst());

    }

    public Mono<UserModel> updateUserTable(final UserModel user) {
        return Mono.just(userTable.updateItem(UpdateItemEnhancedRequest.builder(UserModel.class)
                                                      .item(user)
                                                      .ignoreNulls(true)
                                                      .build()));
    }

    public Mono<UserModel> addUserToTable(final NewUserRequest request,
                                          final String id,
                                          final String folderId) {
        long currentTime = System.currentTimeMillis();
        var userModel = UserModel.builder()
                .username(request.username)
                .displayName(request.displayName)
                .firstName(request.firstName)
                .lastName(request.lastName)
                .email(request.email)
                .id(id)
                .createDate(currentTime)
                .lastModified(currentTime)
                .imageRankingsIds(List.of())
                .rootFolderId(folderId)
                .tags(List.of())
                .build();
        var enhancedRequest = PutItemEnhancedRequest.builder(UserModel.class)
                .item(userModel)
                .build();
        return Mono.fromCallable(() -> {
            userTable.putItemWithResponse(enhancedRequest);
            return userTable.getItem(Key.builder().partitionValue(request.username).build());
        });
    }

    public Mono<String> getUniqueId() {
        return Mono.just(IdGenerator.uniqueIdForIndex(userTable.index("userId_index"), false));
    }

    public record NewUserRequest(String username,
                                 String password,
                                 String firstName,
                                 String lastName,
                                 String displayName,
                                 String email) {

    }
}
