package com.scryer.endpoint.service.user;

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
		var queryEnhancedRequest = QueryEnhancedRequest.builder().queryConditional(queryConditional)
				.attributesToProject().build();
		return Mono.justOrEmpty(this.userTable.index("userId_index").query(queryEnhancedRequest).stream()
				.flatMap(page -> page.items().stream()).findFirst());

	}

	public Mono<UserModel> getUserByUsername(final String username) {
		return Mono.fromCallable(() -> userTable.getItem(Key.builder().partitionValue(username).build()));
	}

	public Mono<UserModel> getUserByEmail(final String email) {
		var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(email).build());
		var queryEnhancedRequest = QueryEnhancedRequest.builder().queryConditional(queryConditional)
				.attributesToProject().build();
		return Mono.justOrEmpty(this.userTable.index("email_index").query(queryEnhancedRequest).stream()
				.flatMap(page -> page.items().stream()).findFirst());

	}

	public Mono<UserModel> updateUser(final UserModel user) {
		return Mono.just(userTable
				.updateItem(UpdateItemEnhancedRequest.builder(UserModel.class).item(user).ignoreNulls(true).build()));
	}

	public Mono<UserModel> addUser(final NewUserRequest request, final String id, final String folderId) {
		var currentTime = System.currentTimeMillis();
		var userModel = UserModel.builder().username(request.username).displayName(request.displayName)
				.firstName(request.firstName).lastName(request.lastName).email(request.email).id(id)
				.createDate(currentTime).lastModified(currentTime).rootFolderId(folderId).build();
		var enhancedRequest = UpdateItemEnhancedRequest.builder(UserModel.class).item(userModel).build();
		return Mono.justOrEmpty(userTable.updateItemWithResponse(enhancedRequest).attributes());
	}

	public Mono<UserModel> getUniqueId(final String username, final String email) {
		var userModel = UserModel.builder().id(IdGenerator.uniqueIdForIndex(userTable.index("userId_index"), false))
				.username(username).email(email).build();

		return Mono
				.justOrEmpty(userTable
						.putItemWithResponse(PutItemEnhancedRequest.builder(UserModel.class).item(userModel).build()))
				.then(Mono.justOrEmpty(userTable.getItem(Key.builder().partitionValue(username).build())));
	}

	public Mono<Boolean> validateUser(final NewUserRequest newUserRequest) {
		return Mono.just(newUserRequest)
				.filter(newUser -> !newUser.username().isEmpty() && !newUser.email().isEmpty()
						&& !newUser.password().isEmpty())
				.flatMap(newUser -> getUserByUsername(newUser.username())).map(Objects::isNull).defaultIfEmpty(true);
	}

	public record NewUserRequest(String username, String password, String firstName, String lastName,
			String displayName, String email) {

	}

}
