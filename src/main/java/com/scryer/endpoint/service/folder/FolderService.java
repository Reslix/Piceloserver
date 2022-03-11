package com.scryer.endpoint.service.folder;

import com.scryer.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.UpdateItemEnhancedRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class FolderService {

	private final DynamoDbTable<FolderModel> folderTable;

	@Autowired
	public FolderService(final DynamoDbTable<FolderModel> folderTable) {
		this.folderTable = folderTable;
	}

	public Mono<FolderModel> createRootFolder(final String userId) {
		return getFoldersByUserId(userId).filter(folder -> folder.getParentFolderIds().isEmpty()).next().map(item -> {
			Mono.error(new IllegalArgumentException("User already has folders"));
			return item;
		}).switchIfEmpty(createFolder(new NewFolderRequest("Gallery", userId, List.of()))
				.switchIfEmpty(Mono.error(new IllegalArgumentException("Failed to create new folder"))));
	}

	public Mono<FolderModel> createFolder(final NewFolderRequest request) {
		String validId = IdGenerator.uniqueIdForTable(folderTable, true);
		Long currentTime = System.currentTimeMillis();
		var newFolder = FolderModel.builder().id(validId).userId(request.userId).name(request.name)
				.createDate(currentTime).lastModified(currentTime).folders(List.of())
				.parentFolderIds(request.parentFolderIds).source(new FolderBaseIdentifier("origin", "self")).build();
		var enhancedRequest = PutItemEnhancedRequest.builder(FolderModel.class).item(newFolder).build();
		return Mono.fromCallable(() -> {
			folderTable.putItemWithResponse(enhancedRequest);
			return folderTable.getItem(Key.builder().partitionValue(validId).build());
		});
	}

	public Mono<Map<String, FolderModel>> getFoldersMapByUserId(final String userId) {
		return getFoldersByUserId(userId).collectMap(FolderModel::getId, folder -> folder);
	}

	public Flux<FolderModel> getFoldersByUserId(final String userId) {
		var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build());
		var queryEnhancedRequest = QueryEnhancedRequest.builder().queryConditional(queryConditional)
				.attributesToProject().build();
		return Flux.fromStream(this.folderTable.index("userId_index").query(queryEnhancedRequest).stream()
				.flatMap(page -> page.items().stream())).parallel().map(this.folderTable::getItem).sequential();
	}

	public Mono<FolderModel> getFolderById(final String folderId) {
		return Mono.justOrEmpty(folderTable.getItem(Key.builder().partitionValue(folderId).build()));
	}

	public Mono<FolderModel> updateFolder(final FolderModel folder) {
		return Mono.just(folderTable.updateItem(
				UpdateItemEnhancedRequest.builder(FolderModel.class).item(folder).ignoreNulls(true).build()));
	}

	public Mono<FolderModel> addChildToParent(final FolderModel child) {
		var parentId = child.getParentFolderIds().get(child.getParentFolderIds().size() - 1);
		var parentMono = getFolderById(parentId).cache();
		return Mono.zip(parentMono, parentMono.map(FolderModel::getFolders), (parent, folders) -> {
			var newFolders = new ArrayList<>(folders);
			newFolders.add(child.getId());
			return folderTable.updateItem(UpdateItemEnhancedRequest.builder(FolderModel.class)
					.item(FolderModel.builder().id(parentId).folders(newFolders).build()).ignoreNulls(true).build());

		});
	}

	public Mono<FolderModel> deleteFolder(final FolderModel folder) {
		return Mono.just(folderTable.deleteItem(folder));
	}

	public record NewFolderRequest(String name, String userId, List<String> parentFolderIds) {

	}

}
