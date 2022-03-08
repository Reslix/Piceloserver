package com.scryer.endpoint.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scryer.endpoint.EndpointApplication;
import com.scryer.endpoint.ScryerTestConfiguration;
import com.scryer.endpoint.security.HandlerTestSecurityConfig;
import com.scryer.endpoint.security.JWTManager;
import com.scryer.model.ddb.ImageSrcModel;
import com.scryer.model.ddb.TagModel;
import com.scryer.model.ddb.UserModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Hooks;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {EndpointApplication.class, HandlerTestSecurityConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TagHandlerTest {


    @MockBean
    private JWTManager jwtManager;

//    @MockBean
//    private TagService tagService;
//
//    @MockBean
//    private UserService userService;
//
//    @MockBean
//    private ImageService imageService;

    @Autowired
    private DynamoDbTable<TagModel> tagTable;

    @Autowired
    private TagHandler handler;

    @Autowired
    private WebTestClient testClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void beforeEach() {
        Hooks.onOperatorDebug();
        Mockito.clearInvocations(jwtManager);//, tagService, userService, imageService);
    }

    @Test
    void testGetTagByName() throws JsonProcessingException {

        TagModel result = TagModel.builder()
                .name("testName")
                .userId("1")
                .id("4")
                .build();
        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));
       // when(tagService.getTag("testTag", "1")).thenReturn(Mono.just(result));
        testClient.get().uri("/api/tag/testTag").exchange().expectBody().json(mapper.writeValueAsString(result));
    }

    @Test
    void testDB() {
        tagTable.putItem(PutItemEnhancedRequest.<TagModel>builder(TagModel.class).item(TagModel.builder().id("1").name("one").build()).build());
        System.out.println(tagTable.scan());

    }

    @Test
    void testUpdateTagImageSrcs() throws JsonProcessingException {
        TagModel tagBefore = TagModel.builder().name("tag1").id("1").imageIds(List.of()).build();
        TagModel tagAfter = TagModel.builder()
                .name("tag1")
                .id("1")
                .imageIds(List.of("image1", "image2", "image3"))
                .build();
        ImageSrcModel image1 = ImageSrcModel.builder().id("image1").tags(List.of()).build();
        ImageSrcModel image2 = ImageSrcModel.builder().id("image2").tags(List.of()).build();
        ImageSrcModel image3 = ImageSrcModel.builder().id("image3").tags(List.of()).build();

        ImageSrcModel image1Updated = ImageSrcModel.builder().id("image1").tags(List.of("tag1")).build();
        ImageSrcModel image2Updated = ImageSrcModel.builder().id("image2").tags(List.of("tag1")).build();
        ImageSrcModel image3Updated = ImageSrcModel.builder().id("image3").tags(List.of("tag1")).build();
        UserModel user1 = UserModel.builder().username("test").id("1").tags(List.of()).build();
        UserModel user2 = UserModel.builder().id("1").username("test").tags(List.of("tag1")).build();
        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));
//        when(userService.getUserByUsername("test")).thenReturn(Mono.just(user1));
//        when(userService.addUserTags(argThat(new UserMatcher(user1)),
//                                     argThat(p -> List.of("tag1")
//                                                          .size() == (p.size())))).thenReturn(Mono.just(user2));
//        when(tagService.addNewTag("tag1", "1")).thenReturn(Mono.just(tagBefore));
//        when(tagService.getTag("tag1", "1")).thenReturn(Mono.just(tagBefore));
//        when(tagService.updateTagImageIds(tagBefore, List.of("image1", "image2", "image3")))
//                .thenReturn(Mono.just(tagAfter));
//        when(imageService.getImageSrc("image1")).thenReturn(Mono.just(image1));
//        when(imageService.getImageSrc("image2")).thenReturn(Mono.just(image2));
//        when(imageService.getImageSrc("image3")).thenReturn(Mono.just(image3));
//        when(imageService.addTagsToImageSrc(argThat(new ImageSrcMatcher(image1)), anyList()))
//                .thenReturn(Mono.just(image1Updated));
//        when(imageService.addTagsToImageSrc(argThat(new ImageSrcMatcher(image2)), anyList()))
//                .thenReturn(Mono.just(image2Updated));
//        when(imageService.addTagsToImageSrc(argThat(new ImageSrcMatcher(image3)), anyList()))
//                .thenReturn(Mono.just(image3Updated));

        testClient.put()
                .uri("/api/image/tag/tag1")
                .body(BodyInserters.fromValue(new TagHandler.UpdateTagImageSrcsRequest(List.of("image1",
                                                                                               "image2",
                                                                                               "image3"), "tag1")))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(mapper.writeValueAsString(List.of(image1Updated, image2Updated, image3Updated)));
//        verify(userService, times(1)).getUserByUsername("test");
//        verify(userService, times(1)).addUserTags(any(), any());
//        verify(tagService, times(1)).addNewTag(any(), any());
//        verify(tagService, times(1)).updateTagImageIds(any(), any());
//        verify(imageService, times(3)).getImageSrc(any());
//        verify(imageService, times(3)).addTagsToImageSrc(any(), any());
    }

    @Test
    void testUpdateImageSrcTags() throws JsonProcessingException {
        TagModel tag1 = TagModel.builder().name("tag1").id("1").imageIds(List.of()).build();
        TagModel tag2 = TagModel.builder().name("tag2").id("2").imageIds(List.of()).build();
        TagModel tag3 = TagModel.builder().name("tag3").id("3").imageIds(List.of()).build();
        TagModel tag1After = TagModel.builder().name("tag1").id("1").imageIds(List.of("image1")).build();
        TagModel tag2After = TagModel.builder().name("tag2").id("2").imageIds(List.of("image1")).build();
        TagModel tag3After = TagModel.builder().name("tag3").id("3").imageIds(List.of("image1")).build();

        ImageSrcModel imageBefore = ImageSrcModel.builder().id("image1").tags(List.of()).build();

        ImageSrcModel imageAfter = ImageSrcModel.builder().id("image1").tags(List.of("tag1", "tag2", "tag3")).build();
        UserModel user1 = UserModel.builder().username("test").id("1").tags(List.of()).build();
        UserModel user2 = UserModel.builder().id("1").username("test").tags(List.of("tag1", "tag2", "tag3")).build();
        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));
//        when(userService.getUserByUsername("test")).thenReturn(Mono.just(user1));
//        when(userService.addUserTags(argThat(new UserMatcher(user1)),
//                                     argThat(p -> List.of("tag1", "tag2", "tag3").size() == (p.size()))))
//                .thenReturn(Mono.just(user2));
//        when(tagService.addNewTag("tag1", "1")).thenReturn(Mono.just(tag1));
//        when(tagService.addNewTag("tag2", "1")).thenReturn(Mono.just(tag2));
//        when(tagService.addNewTag("tag3", "1")).thenReturn(Mono.just(tag3));
//        when(tagService.getTag("tag1", "1")).thenReturn(Mono.just(tag1));
//        when(tagService.getTag("tag2", "1")).thenReturn(Mono.just(tag2));
//        when(tagService.getTag("tag3", "1")).thenReturn(Mono.just(tag3));
//        when(tagService.updateTagImageIds(tag1, List.of("image1")))
//                .thenReturn(Mono.just(tag1After));
//        when(tagService.updateTagImageIds(tag2, List.of("image1")))
//                .thenReturn(Mono.just(tag2After));
//        when(tagService.updateTagImageIds(tag3, List.of("image1")))
//                .thenReturn(Mono.just(tag3After));
//        when(imageService.getImageSrc("image1")).thenReturn(Mono.just(imageBefore));
//        when(imageService.addTagsToImageSrc(argThat(new ImageSrcMatcher(imageBefore)), anyList()))
//                .thenReturn(Mono.just(imageAfter));

        testClient.put()
                .uri("/api/tag/image/image1")
                .body(BodyInserters.fromValue(new TagHandler.UpdateImageSrcTagsRequest("image1",
                                                                                       List.of("tag1",
                                                                                               "tag2",
                                                                                               "tag3"))))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(mapper.writeValueAsString(imageAfter));
//        verify(imageService, times(1)).addTagsToImageSrc(any(), any());
    }

    @Test
    void testDeleteImageSrcTags_BaseCase() throws JsonProcessingException {
        TagModel tag1 = TagModel.builder().name("tag1").id("1")
                .imageIds(List.of("image1", "image2")).imageRankingIds(List.of()).build();
        TagModel tag2 = TagModel.builder().name("tag2").id("2")
                .imageIds(List.of("image1", "image2")).imageRankingIds(List.of()).build();
        TagModel tag3 = TagModel.builder().name("tag3").id("3")
                .imageIds(List.of("image1", "image2")).imageRankingIds(List.of()).build();
        TagModel tag1After = TagModel.builder().name("tag1").id("1")
                .imageIds(List.of("image2")).imageRankingIds(List.of()).build();
        TagModel tag3After = TagModel.builder().name("tag3").id("3")
                .imageIds(List.of("image2")).imageRankingIds(List.of()).build();
        ImageSrcModel imageBefore = ImageSrcModel.builder().id("image1").tags(List.of("tag1", "tag2", "tag3")).build();
        ImageSrcModel imageAfter = ImageSrcModel.builder().id("image1").tags(List.of("tag2")).build();

        UserModel user1 = UserModel.builder().username("test").id("1").tags(List.of("tag1", "tag2", "tag3")).build();
        UserModel user2 = UserModel.builder().id("1").username("test").tags(List.of("tag2")).build();

        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));
//        when(userService.getUserByUsername("test")).thenReturn(Mono.just(user1));
//        when(userService.deleteUserTags(argThat(new UserMatcher(user1)), anyList())).thenReturn(Mono.just(user2));
//
//        when(tagService.getTag("tag1", "1")).thenReturn(Mono.just(tag1));
//        when(tagService.getTag("tag2", "1")).thenReturn(Mono.just(tag2));
//        when(tagService.getTag("tag3", "1")).thenReturn(Mono.just(tag3));
//        when(tagService.deleteTagImages(tag1, List.of("image1"))).thenReturn(Mono.just(tag1After));
//        when(tagService.deleteTagImages(tag3, List.of("image1"))).thenReturn(Mono.just(tag3After));
//        when(imageService.getImageSrc("image1")).thenReturn(Mono.just(imageBefore));
//        when(imageService.deleteImageSrcTags(argThat(new ImageSrcMatcher(imageBefore)), anyList()))
//                .thenReturn(Mono.just(imageAfter));

        testClient.method(HttpMethod.DELETE)
                .uri("/api/image/tag/")
                .body(BodyInserters.fromValue(new TagHandler.DeleteImageSrcTagsRequest("image1",
                                                                                       List.of("tag1", "tag3"))))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(mapper.writeValueAsString(imageAfter));
    }

    @Test
    void testDeleteTagImageSrcs_BaseCase() throws JsonProcessingException {
        TagModel tagBefore = TagModel.builder().name("tag1").id("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tagAfter = TagModel.builder().name("tag1").id("1")
                .imageIds(List.of("image2")).imageRankingIds(List.of()).build();

        ImageSrcModel image1 = ImageSrcModel.builder().id("image1").tags(List.of("tag1", "tag2", "tag3")).build();
        ImageSrcModel image2 = ImageSrcModel.builder().id("image2").tags(List.of("tag1", "tag2", "tag3")).build();
        ImageSrcModel image3 = ImageSrcModel.builder().id("image3").tags(List.of("tag1", "tag2", "tag3")).build();
        ImageSrcModel image1After = ImageSrcModel.builder().id("image1").tags(List.of("tag2", "tag3")).build();
        ImageSrcModel image3After = ImageSrcModel.builder().id("image3").tags(List.of("tag2", "tag3")).build();

        UserModel user1 = UserModel.builder().username("test").id("1").tags(List.of("tag1", "tag2", "tag3")).build();
        UserModel user2 = UserModel.builder().id("1").username("test").tags(List.of("tag2")).build();

        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));
//        when(userService.getUserByUsername("test")).thenReturn(Mono.just(user1));
//        when(userService.deleteUserTags(argThat(new UserMatcher(user1)), anyList())).thenReturn(Mono.just(user2));
//
//        when(tagService.getTag("tag1", "1")).thenReturn(Mono.just(tagBefore));
//        when(tagService.deleteTagImages(argThat(new TagMatcher(tagBefore)), anyList())).thenReturn(Mono.just(tagAfter));
//        when(imageService.getImageSrc("image1")).thenReturn(Mono.just(image1));
//        when(imageService.getImageSrc("image2")).thenReturn(Mono.just(image2));
//        when(imageService.getImageSrc("image3")).thenReturn(Mono.just(image3));
//        when(imageService.deleteImageSrcTags(argThat(new ImageSrcMatcher(image1)), anyList()))
//                .thenReturn(Mono.just(image1After));
//        when(imageService.deleteImageSrcTags(argThat(new ImageSrcMatcher(image3)), anyList()))
//                .thenReturn(Mono.just(image3After));

        testClient.method(HttpMethod.DELETE)
                .uri("/api/tag/image")
                .body(BodyInserters.fromValue(new TagHandler.DeleteTagImageSrcsRequest(List.of("image1", "image3"),
                                                                                       "tag1")))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .json(mapper.writeValueAsString(List.of(image1After, image3After)));

    }
}