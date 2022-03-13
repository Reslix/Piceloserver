package com.scryer.endpoint.configuration;

import com.scryer.endpoint.handler.FolderHandler;
import com.scryer.endpoint.handler.ImageHandler;
import com.scryer.endpoint.handler.ImageRankingHandler;
import com.scryer.endpoint.handler.LoginHandler;
import com.scryer.endpoint.handler.TagHandler;
import com.scryer.endpoint.handler.UserHandler;
import com.scryer.endpoint.metrics.RouteMetricsFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class ScryerRouterConfiguration {

    private final RouteMetricsFilter routeMetricsFilter;

    @Autowired
    public ScryerRouterConfiguration(final RouteMetricsFilter routeMetricsFilter) {
        this.routeMetricsFilter = routeMetricsFilter;
    }

    @Bean
    public RouterFunction<ServerResponse> imageRankingRoute(final ImageRankingHandler imageRankingHandler) {
        var postImageRankingPredicate = RequestPredicates.POST("/api/imageranking/")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var postRankingStepsPredicate = RequestPredicates.POST("/api/imageranking/step/")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var getUserImageRankingsPredicate = RequestPredicates.GET("/api/imageranking/user/{userId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var getRankingStepsPredicate = RequestPredicates.GET("/api/imageranking/step/{imageRankingId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var updateImageRankingRankingStepPredicate = RequestPredicates.PUT("/api/imageranking/step/{imageRankingId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var updateImageRankingImagesPredicate = RequestPredicates.PUT("/api/imageranking/images/{imageRankingId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteImageRankingPredicate = RequestPredicates.DELETE("/api/imageranking/")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));

        return RouterFunctions.route(postImageRankingPredicate, imageRankingHandler::createImageRanking)
                .andRoute(postRankingStepsPredicate, imageRankingHandler::createRankingsSteps)
                .andRoute(getUserImageRankingsPredicate, imageRankingHandler::getUserImageRankings)
                .andRoute(getRankingStepsPredicate, imageRankingHandler::getRankingSteps)
                .andRoute(updateImageRankingRankingStepPredicate, imageRankingHandler::updateImageRankingRankingStep)
                .andRoute(updateImageRankingImagesPredicate, imageRankingHandler::updateImageRankingImages)
                .andRoute(deleteImageRankingPredicate, imageRankingHandler::deleteImageRanking).filter(routeMetricsFilter);
    }
    @Bean
    public RouterFunction<ServerResponse> imageRoute(final ImageHandler imageHandler) {
        var postImagePredicate = RequestPredicates.POST("/api/image")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var getImagesByFolderPredicate = RequestPredicates.GET("/api/images/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var moveImagesPredicate = RequestPredicates.PUT("/api/images/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteImagePredicate = RequestPredicates.DELETE("/api/image")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteImagesPredicate = RequestPredicates.DELETE("/api/images")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));

        return RouterFunctions.route(postImagePredicate, imageHandler::postImage)
                .andRoute(getImagesByFolderPredicate, imageHandler::getImagesByFolder)
                .andRoute(moveImagesPredicate, imageHandler::moveImages)
                .andRoute(deleteImagePredicate, imageHandler::deleteImage)
                .andRoute(deleteImagesPredicate, imageHandler::deleteImages).filter(routeMetricsFilter);
    }

    @Bean
    public RouterFunction<ServerResponse> folderRoute(final FolderHandler folderHandler) {
        var getFolderPredicate = RequestPredicates.GET("/api/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var getFolderByUserIdPredicate = RequestPredicates.GET("/api/folder/user/{userId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var createFolderPredicate = RequestPredicates.POST("/api/folder")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var moveFolderPredicate = RequestPredicates.PUT("/api/folder/{folderId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteFolderPredicate = RequestPredicates.DELETE("/api/folder")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));

        return RouterFunctions.route(getFolderPredicate, folderHandler::getFolderById)
                .andRoute(getFolderByUserIdPredicate, folderHandler::getFolderMapByUserId)
                .andRoute(createFolderPredicate, folderHandler::postNewFolder)
                .andRoute(moveFolderPredicate, folderHandler::moveFolder)
                .andRoute(deleteFolderPredicate, folderHandler::deleteFolder).filter(routeMetricsFilter);
    }

    @Bean
    public RouterFunction<ServerResponse> tagRoute(final TagHandler tagHandler) {
        var getTagPredicate = RequestPredicates.GET("/api/tag/{tagName}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var getUserTagsPredicate = RequestPredicates.GET("/api/tags/user/{userId}")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var updateImageSrcTagsPredicate = RequestPredicates.POST("/api/tags/images/")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var updateImageRankingTagsPredicate = RequestPredicates.POST("/api/tags/imageranking/")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteImageSrcTagsPredicate = RequestPredicates.DELETE("/api/tags/images/")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));
        var deleteImageRankingTagsPredicate = RequestPredicates.DELETE("/api/tags/imageranking/")
                .and(RequestPredicates.accept(MediaType.APPLICATION_JSON));

        return RouterFunctions.route(getTagPredicate, tagHandler::getTagByName)
                .andRoute(getUserTagsPredicate, tagHandler::getUserTags)
                .andRoute(updateImageSrcTagsPredicate, tagHandler::updateImageSrcTags)
                .andRoute(updateImageRankingTagsPredicate, tagHandler::updateImageRankingTags)
                .andRoute(deleteImageSrcTagsPredicate, tagHandler::deleteImageSrcTags)
                .andRoute(deleteImageRankingTagsPredicate, tagHandler::deleteImageRankingTags).filter(routeMetricsFilter);
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
                .andRoute(checkEmailPredicate, userHandler::getCheckEmail).filter(routeMetricsFilter);
    }

    @Bean
    public RouterFunction<ServerResponse> loginRoute(final LoginHandler loginHandler) {
        var loginPredicate = RequestPredicates.POST("/auth/login");
        var logoutPredicate = RequestPredicates.POST("/auth/logout");

        return RouterFunctions.route(loginPredicate, loginHandler::login)
                .andRoute(logoutPredicate, loginHandler::logout).filter(routeMetricsFilter);
    }

}
