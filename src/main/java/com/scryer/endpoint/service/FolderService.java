package com.scryer.endpoint.service;

import com.scryer.model.ddb.BaseIdentifier;
import com.scryer.model.ddb.FolderModel;
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

import java.util.List;
import java.util.Map;

@Service
public class FolderService {
    private final DynamoDbTable<FolderModel> folderTable;

    @Autowired
    public FolderService(final DynamoDbTable<FolderModel> folderTable) {
        this.folderTable = folderTable;
    }

    /**
     * @param userId
     * @return
     */
    public Mono<FolderModel> createRootFolderInTable(final String userId) {
        return getFoldersByUserIdFromTable(userId).filter(folder -> folder.getParentFolderIds().isEmpty())
                .next()
                .map(item -> {
                    Mono.error(new IllegalArgumentException("User already has folders"));
                    return item;
                })
                .switchIfEmpty(createFolderInTable(new NewFolderRequest("Gallery", userId, List.of()))
                                       .switchIfEmpty(Mono.error(new IllegalArgumentException(
                                               "Failed to create new folder"))));
    }

    /**
     * @param request
     * @return
     */
    public Mono<FolderModel> createFolderInTable(final NewFolderRequest request) {
        String validId = IdGenerator.uniqueIdForTable(folderTable, true);
        Long currentTime = System.currentTimeMillis();
        var newFolder = FolderModel.builder()
                .id(validId)
                .userId(request.userId)
                .name(request.name)
                .createDate(currentTime)
                .lastModified(currentTime)
                .folders(List.of())
                .parentFolderIds(request.parentFolderIds)
                .source(new BaseIdentifier("origin", "self"))
                .tags(List.of())
                .build();
        var enhancedRequest = PutItemEnhancedRequest.builder(FolderModel.class).item(newFolder).build();
        return Mono.fromCallable(() -> {
            folderTable.putItemWithResponse(enhancedRequest);
            return folderTable.getItem(Key.builder().partitionValue(validId).build());
        });
    }

    /**
     * @param userId
     * @return
     */
    public Mono<Map<String, FolderModel>> getFoldersMapByUserIdFromTable(final String userId) {
        return getFoldersByUserIdFromTable(userId).collectMap(FolderModel::getId, folder -> folder);
    }

    /**
     * @param userId
     * @return
     */
    public Flux<FolderModel> getFoldersByUserIdFromTable(final String userId) {
        var queryConditional = QueryConditional.keyEqualTo(Key.builder().partitionValue(userId).build());
        var queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .attributesToProject()
                .build();
        return Flux.fromStream(this.folderTable.index("userId_index")
                                       .query(queryEnhancedRequest)
                                       .stream()
                                       .flatMap(page -> page.items().stream()))
                .parallel()
                .map(this.folderTable::getItem).sequential();
    }

    /**
     * @param folderId
     * @return
     */
    public Mono<FolderModel> getFolderByIdFromTable(final String folderId) {
        return Mono.justOrEmpty(folderTable.getItem(Key.builder().partitionValue(folderId).build()));
    }

    /**
     * @param folder
     * @return
     */
    public Mono<FolderModel> updateFolderTable(final FolderModel folder) {
        return Mono.just(folderTable.updateItem(UpdateItemEnhancedRequest.builder(FolderModel.class)
                                                        .item(folder)
                                                        .ignoreNulls(true)
                                                        .build()));
    }

    /**
     * @param folder
     * @return
     */
    public Mono<FolderModel> deleteFolderFromTable(final FolderModel folder) {
        return Mono.just(folderTable.deleteItem(folder));
    }

    public record NewFolderRequest(String name,
                                   String userId,
                                   List<String> parentFolderIds) {

    }
}
