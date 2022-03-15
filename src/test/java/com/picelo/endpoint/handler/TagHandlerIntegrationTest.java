package com.picelo.endpoint.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.picelo.endpoint.EndpointApplication;
import com.picelo.endpoint.configuration.PiceloTestConfiguration;
import com.picelo.endpoint.security.HandlerTestSecurityConfig;
import com.picelo.endpoint.security.JWTManager;
import com.picelo.endpoint.service.tag.TagService;
import com.picelo.endpoint.service.imagesrc.ImageSrc;
import com.picelo.endpoint.service.tag.TagModel;
import com.picelo.endpoint.service.user.User;
import com.picelo.util.TagMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Hooks;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration test because unit tests will destroy my tendons
 */
@SpringBootTest(classes = {EndpointApplication.class, HandlerTestSecurityConfig.class, PiceloTestConfiguration.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {"spring.main.allow-bean-definition-overriding=true"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TagHandlerIntegrationTest {

    @MockBean
    private JWTManager jwtManager;

    @Autowired
    private DynamoDbTable<TagModel> tagTable;

    @Autowired
    private DynamoDbTable<User> userTable;

    @Autowired
    private DynamoDbTable<ImageSrc> imageTable;

    @Autowired
    private TagService tagService;

    @Autowired
    private TagHandler tagHandler;

    @Autowired
    private WebTestClient testClient;

    @Autowired
    private ReactiveRedisTemplate<String, TagModel> tagRedisTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void beforeEach() {
        Hooks.onOperatorDebug();
        Mockito.clearInvocations(jwtManager);
    }

    @AfterEach
    void afterEach() {
        tagTable.scan().items().stream().forEach(item -> tagTable.deleteItem(item));
        imageTable.scan().items().stream().forEach(item -> imageTable.deleteItem(item));
        userTable.scan().items().stream().forEach(item -> userTable.deleteItem(item));
        tagRedisTemplate.scan().map(tag -> tagRedisTemplate.delete(tag)).collectList().block();
    }

    @Test
    void testGetTagByName() throws JsonProcessingException {
        TagModel result = TagModel.builder().name("testName").userId("1").id("4").imageRankingIds(List.of())
                .imageIds(List.of()).build();

        tagTable.putItem(PutItemEnhancedRequest.<TagModel>builder(TagModel.class).item(result).build());
        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));
        testClient.get().uri("/api/tag/testName").exchange().expectBody().json(mapper.writeValueAsString(result));
    }

    @Test
    void testGetUserTags() throws JsonProcessingException {
        TagModel tag1 = TagModel.builder().name("tag1").userId("1").id("1").imageRankingIds(List.of())
                .imageIds(List.of()).build();
        TagModel tag2 = TagModel.builder().name("tag2").userId("1").id("2").imageRankingIds(List.of())
                .imageIds(List.of()).build();
        TagModel tag3 = TagModel.builder().name("tag3").userId("1").id("3").imageRankingIds(List.of())
                .imageIds(List.of()).build();
        TagModel tag4 = TagModel.builder().name("tag4").userId("1").id("4").imageRankingIds(List.of())
                .imageIds(List.of()).build();

        tagTable.putItem(PutItemEnhancedRequest.<TagModel>builder(TagModel.class).item(tag1).build());
        tagTable.putItem(PutItemEnhancedRequest.<TagModel>builder(TagModel.class).item(tag2).build());
        tagTable.putItem(PutItemEnhancedRequest.<TagModel>builder(TagModel.class).item(tag3).build());
        tagTable.putItem(PutItemEnhancedRequest.<TagModel>builder(TagModel.class).item(tag4).build());
        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));
        testClient.get().uri("/api/tags/user/1").exchange().expectBody()
                .json(mapper.writeValueAsString(List.of(tag1, tag2, tag3, tag4)));
    }

    @Test
    void testUpdateTagImageSrcs_AddTwo()  {
        TagModel tag1Before = TagModel.builder().name("tag1").userId("1").id("1").imageRankingIds(List.of())
                .imageIds(List.of()).build();
        TagModel tag2Before = TagModel.builder().name("tag2").userId("1").id("2").imageRankingIds(List.of())
                .imageIds(List.of()).build();
        TagModel tag1After = TagModel.builder().name("tag1").userId("1").id("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tag2After = TagModel.builder().name("tag2").userId("1").id("2").imageIds(List.of("image1", "image3"))
                .imageRankingIds(List.of()).build();
        User user1 = User.builder().username("test").id("1").build();

        userTable.putItem(user1);

        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));

        EntityExchangeResult<byte[]> result1Bytes = testClient.post().uri("/api/tags/images/")
                .body(BodyInserters.fromValue(
                        new TagHandler.UpdateImageSrcTagsRequest(List.of("image1", "image2", "image3"),
                                                                 List.of("tag1"))))
                .exchange().expectStatus().isOk().expectBody().returnResult();
        // Test for duplication
        testClient.post().uri("/api/tags/images/")
                .body(BodyInserters
                              .fromValue(new TagHandler.UpdateImageSrcTagsRequest(List.of("image1", "image3"),
                                                                                  List.of("tag2"))))
                .exchange().expectStatus().isOk().expectBody().returnResult();
        EntityExchangeResult<byte[]> result2Bytes = testClient.post().uri("/api/tags/images/")
                .body(BodyInserters.fromValue(new TagHandler.UpdateImageSrcTagsRequest(List.of("image1", "image3"),
                                                                                       List.of("tag2"))))
                .exchange().expectStatus().isOk().expectBody().returnResult();

        TagModel tag1Result = tagService.getTag("tag1", "1").block();
        TagModel tag2Result = tagService.getTag("tag2", "1").block();
        assertTrue(new TagMatcher(tag1After).matches(tag1Result));
        assertTrue(new TagMatcher(tag2After).matches(tag2Result));
    }

    @Test
    void testUpdateImageSrcTags() {
        TagModel tag1After = TagModel.builder().name("tag1").id("1").userId("1").imageIds(List.of("image1"))
                .imageRankingIds(List.of()).build();
        TagModel tag2After = TagModel.builder().name("tag2").id("2").userId("1").imageIds(List.of("image1"))
                .imageRankingIds(List.of()).build();
        TagModel tag3After = TagModel.builder().name("tag3").id("3").userId("1").imageIds(List.of("image1"))
                .imageRankingIds(List.of()).build();

        User user1 = User.builder().username("test").id("1").build();

        userTable.putItem(user1);
        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));

        EntityExchangeResult<byte[]> result1Bytes = testClient.post().uri("/api/tags/images/")
                .body(BodyInserters
                              .fromValue(new TagHandler.UpdateImageSrcTagsRequest(List.of("image1"),
                                                                                  List.of("tag1", "tag2"))))
                .exchange().expectStatus().isOk().expectBody().returnResult();

        EntityExchangeResult<byte[]> result2Bytes = testClient.post().uri("/api/tags/images/")
                .body(BodyInserters.fromValue(new TagHandler.UpdateImageSrcTagsRequest(List.of("image1"),
                                                                                       List.of("tag1",
                                                                                               "tag2",
                                                                                               "tag3"))))
                .exchange().expectStatus().isOk().expectBody().returnResult();

        TagModel tag1Result = tagService.getTag("tag1", "1").block();
        TagModel tag2Result = tagService.getTag("tag2", "1").block();
        TagModel tag3Result = tagService.getTag("tag3", "1").block();
        assertTrue(new TagMatcher(tag1After).matches(tag1Result));
        assertTrue(new TagMatcher(tag2After).matches(tag2Result));
        assertTrue(new TagMatcher(tag3After).matches(tag3Result));
    }

    @Test
    void testDeleteImageSrcTags_BaseCase() {
        TagModel tag1 = TagModel.builder().name("tag1").id("1").userId("1").imageIds(List.of("image1"))
                .imageRankingIds(List.of()).build();
        TagModel tag2 = TagModel.builder().name("tag2").id("2").userId("1").imageIds(List.of("image1", "image2"))
                .imageRankingIds(List.of()).build();
        TagModel tag3 = TagModel.builder().name("tag3").id("3").userId("1").imageIds(List.of("image1", "image2"))
                .imageRankingIds(List.of()).build();
        TagModel tag2After = TagModel.builder().name("tag2").id("2").userId("1").imageIds(List.of("image1", "image2"))
                .imageRankingIds(List.of()).build();
        TagModel tag3After = TagModel.builder().name("tag3").id("3").userId("1").imageIds(List.of("image2"))
                .imageRankingIds(List.of()).build();

        User user1 = User.builder().username("test").id("1").build();

        tagTable.putItem(tag1);
        tagTable.putItem(tag2);
        tagTable.putItem(tag3);
        userTable.putItem(user1);

        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));

        EntityExchangeResult<byte[]> resultBytes = testClient.method(HttpMethod.DELETE).uri("/api/tags/images/")
                .body(BodyInserters.fromValue(new TagHandler.DeleteTagImageSrcsRequest(List.of("image1"),
                                                                                       List.of("tag1", "tag3"))))
                .exchange().expectStatus().isOk().expectBody().returnResult();

        TagModel tag1Result = tagService.getTag("tag1", "1").block();
        TagModel tag2Result = tagService.getTag("tag2", "1").block();
        TagModel tag3Result = tagService.getTag("tag3", "1").block();
        assertNull(tag1Result);
        assertTrue(new TagMatcher(tag2After).matches(tag2Result));
        assertTrue(new TagMatcher(tag3After).matches(tag3Result));
    }

    @Test
    void testDeleteTagImageSrcs_BaseCase() throws IOException {
        TagModel tag1Before = TagModel.builder().name("tag1").id("1").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tag2Before = TagModel.builder().name("tag2").id("2").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tag3Before = TagModel.builder().name("tag3").id("3").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tag1After = TagModel.builder().name("tag1").id("1").userId("1").imageIds(List.of("image2"))
                .imageRankingIds(List.of()).build();
        TagModel tag2After = TagModel.builder().name("tag2").id("2").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tag3After = TagModel.builder().name("tag3").id("3").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();

        User user1 = User.builder().username("test").id("1").build();

        tagTable.putItem(tag1Before);
        tagTable.putItem(tag2Before);
        tagTable.putItem(tag3Before);
        userTable.putItem(user1);

        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));

        EntityExchangeResult<byte[]> result1Bytes = testClient.method(HttpMethod.DELETE).uri("/api/tags/images/")
                .body(BodyInserters.fromValue(new TagHandler.DeleteTagImageSrcsRequest(List.of("image1", "image3"),
                                                                                       List.of("tag1"))))
                .exchange().expectStatus().isOk().expectBody().returnResult();

        List<ImageSrc> result1 = mapper.readValue(result1Bytes.getResponseBody(),
                                                  new TypeReference<List<ImageSrc>>() {
                                                       });
        TagModel tag1Result = tagService.getTag("tag1", "1").block();
        TagModel tag2Result = tagService.getTag("tag2", "1").block();
        TagModel tag3Result = tagService.getTag("tag3", "1").block();
        assertTrue(new TagMatcher(tag1After).matches(tag1Result));
        assertTrue(new TagMatcher(tag2After).matches(tag2Result));
        assertTrue(new TagMatcher(tag3After).matches(tag3Result));

        tagTable.deleteItem(tag1Result);
        tagTable.deleteItem(tag2Result);
        tagTable.deleteItem(tag3Result);
        userTable.deleteItem(user1);
    }
}