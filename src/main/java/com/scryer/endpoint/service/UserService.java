package com.scryer.endpoint.service;

import com.scryer.model.ddb.UserModel;
import com.scryer.util.ConsolidateUtil;
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

import java.util.List;
import java.util.Objects;

@Service
public class UserService {

    private final DynamoDbTable<UserModel> userTable;

    @Autowired
    public UserService(final DynamoDbTable<UserModel> userTable) {
        this.userTable = userTable;
    }

    public Mono<UserModel> getUserById(final String id) {
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

    public Mono<UserModel> getUserByUsername(final String username) {
        return Mono.fromCallable(() -> userTable.getItem(Key.builder().partitionValue(username).build()));
    }

    public Mono<UserModel> getUserByEmail(final String email) {
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

    public Mono<UserModel> updateUser(final UserModel user) {
        return Mono.just(userTable.updateItem(UpdateItemEnhancedRequest.builder(UserModel.class)
                                                      .item(user)
                                                      .ignoreNulls(true)
                                                      .build()));
    }

    public Mono<UserModel> addUserTags(final UserModel user, final List<String> tags) {
        var newTags = ConsolidateUtil.getCombinedTags(user, tags);
        if (newTags.size() != user.getTags().size()) {
            return updateUser(UserModel.builder()
                                      .id(user.getId())
                                      .username(user.getUsername())
                                      .lastModified(System.currentTimeMillis())
                                      .tags(newTags)
                                      .build());

        }
        return Mono.just(user);
    }

    public Mono<UserModel> deleteUserTags(final UserModel user, final List<String> tags) {
        var newTags = ConsolidateUtil.getSubtractedTags(user, tags);
        if (newTags.size() != user.getTags().size()) {
            return updateUser(UserModel.builder()
                                      .id(user.getId())
                                      .username(user.getUsername())
                                      .lastModified(System.currentTimeMillis())
                                      .tags(newTags)
                                      .build());

        }
        return Mono.just(user);
    }

    public Mono<UserModel> addUser(final NewUserRequest request,
                                   final String id,
                                   final String folderId) {
        var currentTime = System.currentTimeMillis();
        var userModel = UserModel.builder()
                .username(request.username)
                .displayName(request.displayName)
                .firstName(request.firstName)
                .lastName(request.lastName)
                .email(request.email)
                .id(id)
                .createDate(currentTime)
                .lastModified(currentTime)
                .rootFolderId(folderId)
                .build();
        var enhancedRequest = UpdateItemEnhancedRequest.builder(UserModel.class)
                .item(userModel)
                .build();
        return Mono.justOrEmpty(userTable.updateItemWithResponse(enhancedRequest).attributes());
    }

    public Mono<UserModel> getUniqueId(final String username, final String email) {
        var userModel = UserModel.builder()
                .id(IdGenerator.uniqueIdForIndex(userTable.index("userId_index"), false))
                .username(username)
                .email(email)
                .build();

        return Mono.justOrEmpty(userTable.putItemWithResponse(PutItemEnhancedRequest
                                                                      .builder(UserModel.class)
                                                                      .item(userModel)
                                                                      .build()))
                .then(Mono.justOrEmpty(userTable.getItem(Key.builder().partitionValue(username).build())));
    }

    public Mono<Boolean> validateUser(final NewUserRequest newUserRequest) {
        return Mono.just(newUserRequest).filter(newUser -> !newUser.username().isEmpty()
                                   && !newUser.email().isEmpty()
                                   && !newUser.password().isEmpty())
                .flatMap(newUser -> getUserByUsername(newUser.username()))
                .map(Objects::isNull)
                .defaultIfEmpty(true);
    }

    public record NewUserRequest(String username,
                                 String password,
                                 String firstName,
                                 String lastName,
                                 String displayName,
                                 String email) {

    }
}
