package com.scryer.endpoint.configuration;

import com.scryer.endpoint.handler.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ScryerRouterConfiguration {

    @Bean
    public RouterFunction<ServerResponse> imageRoute(final ImageHandler imageHandler) {
        var postImagePredicate = RequestPredicates.POST("/api/image")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var getImagesByFolderPredicate = RequestPredicates.GET("/api/image/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        return RouterFunctions.route(postImagePredicate, imageHandler::postImage)
                .andRoute(getImagesByFolderPredicate, imageHandler::getImagesByFolder);
    }

    @Bean
    public RouterFunction<ServerResponse> folderRoute(final FolderHandler folderHandler) {
        var getFolderPredicate = RequestPredicates.GET("/api/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var getFolderByUserIdPredicate = RequestPredicates.GET("/api/folder/user/{userId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var createFolderPredicate = RequestPredicates.POST("/api/folder")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var updateFolderPredicate = RequestPredicates.PUT("/api/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteFolderPredicate = RequestPredicates.DELETE("/api/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        return RouterFunctions.route(getFolderPredicate, folderHandler::getFolderById)
                .andRoute(getFolderByUserIdPredicate, folderHandler::getFolderMapByUserId)
                .andRoute(createFolderPredicate, folderHandler::postNewFolder)
                .andRoute(updateFolderPredicate, folderHandler::putUpdateFolder)
                .andRoute(deleteFolderPredicate, folderHandler::deleteFolder);
    }

    @Bean
    public RouterFunction<ServerResponse> tagRoute(final TagHandler tagHandler) {
        var getTagPredicate = RequestPredicates.GET("/api/tag/{tagName}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var postTagPredicate = RequestPredicates.POST("/api/tag")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteTagsPredicate = RequestPredicates.DELETE("/api/tag")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var updateTagsForFolderPredicate = RequestPredicates.PUT("/api/tag/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var updateTagsForImageSrcPredicate = RequestPredicates.PUT("/api/tag/image/{imageId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteTagsFromFolderPredicate = RequestPredicates.DELETE("/api/tag/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteTagsFromImageSrcPredicate = RequestPredicates.DELETE("/api/tag/imageSrc/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));

        return RouterFunctions.route(getTagPredicate, tagHandler::getTagByName)
                .andRoute(postTagPredicate, tagHandler::postNewTags)
                .andRoute(deleteTagsPredicate, tagHandler::deleteTags)
                .andRoute(updateTagsForFolderPredicate, tagHandler::updateTagsForFolder)
                .andRoute(updateTagsForImageSrcPredicate, tagHandler::updateTagsForImageSrc)
                .andRoute(deleteTagsFromFolderPredicate, tagHandler::deleteTagsFromFolder)
                .andRoute(deleteTagsFromImageSrcPredicate, tagHandler::deleteTagsFromImageSrc);
    }

    @Bean
    public RouterFunction<ServerResponse> userRoute(final UserHandler userHandler) {
        var getUserPredicate = RequestPredicates.GET("/api/user/info/{username}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var createUserPredicate = RequestPredicates.POST("/auth/user")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var checkUsernamePredicate = RequestPredicates.GET("/auth/user/username/{username}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var checkEmailPredicate = RequestPredicates.GET("/auth/user/email/{email}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        return RouterFunctions.route(getUserPredicate, userHandler::getUserByUsername)
                .andRoute(createUserPredicate, userHandler::postNewUser)
                .andRoute(checkUsernamePredicate, userHandler::getCheckUsername)
                .andRoute(checkEmailPredicate, userHandler::getCheckEmail);
    }

    @Bean
    public RouterFunction<ServerResponse> authRoute(final LoginHandler loginHandler) {
        var loginPredicate = RequestPredicates.POST("/auth/login");
        var logoutPredicate = RequestPredicates.POST("/auth/logout");
        return RouterFunctions.route(loginPredicate, loginHandler::login)
                .andRoute(logoutPredicate, loginHandler::logout);
    }

}
