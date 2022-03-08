package com.scryer.endpoint.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scryer.endpoint.EndpointApplication;
import com.scryer.endpoint.security.HandlerTestSecurityConfig;
import com.scryer.endpoint.security.JWTManager;
import com.scryer.endpoint.service.TagService;
import com.scryer.model.ddb.ImageSrcModel;
import com.scryer.model.ddb.TagModel;
import com.scryer.model.ddb.UserModel;
import com.scryer.util.ImageSrcMatcher;
import com.scryer.util.TagMatcher;
import com.scryer.util.UserMatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Hooks;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test because unit tests will destroy my tendons
 */
@SpringBootTest(classes = {EndpointApplication.class, HandlerTestSecurityConfig.class},
                webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TagHandlerIntegrationTest {


    @MockBean
    private JWTManager jwtManager;

    @Autowired
    private DynamoDbTable<TagModel> tagTable;

    @Autowired
    private DynamoDbTable<UserModel> userTable;

    @Autowired
    private DynamoDbTable<ImageSrcModel> imageTable;

    @Autowired
    private TagService tagService;

    @Autowired
    private TagHandler tagHandler;

    @Autowired
    private WebTestClient testClient;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void beforeEach() {
        Hooks.onOperatorDebug();
        Mockito.clearInvocations(jwtManager);
    }

    @Test
    void testGetTagByName() throws JsonProcessingException {
        TagModel result = TagModel.builder()
                .name("testName")
                .userId("1")
                .id("4")
                .imageRankingIds(List.of())
                .imageIds(List.of())
                .build();

        tagTable.putItem(PutItemEnhancedRequest.<TagModel>builder(TagModel.class).item(result).build());
        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));
        testClient.get().uri("/api/tag/testName").exchange().expectBody().json(mapper.writeValueAsString(result));
    }

    @Test
    void testUpdateTagImageSrcs_AddTwo() throws IOException {
        TagModel tag1After = TagModel.builder()
                .name("tag1")
                .userId("1")
                .id("1")
                .imageIds(List.of("image1", "image2", "image3"))
                .imageRankingIds(List.of())
                .build();
        TagModel tag2After = TagModel.builder()
                .name("tag2")
                .userId("1")
                .id("2")
                .imageIds(List.of("image1", "image3"))
                .imageRankingIds(List.of())
                .build();
        ImageSrcModel image1 = ImageSrcModel.builder().id("image1").tags(List.of()).build();
        ImageSrcModel image2 = ImageSrcModel.builder().id("image2").tags(List.of()).build();
        ImageSrcModel image3 = ImageSrcModel.builder().id("image3").tags(List.of()).build();

        ImageSrcModel image1Updated = ImageSrcModel.builder().id("image1").tags(List.of("tag1", "tag2")).build();
        ImageSrcModel image2Updated = ImageSrcModel.builder().id("image2").tags(List.of("tag1")).build();
        ImageSrcModel image3Updated = ImageSrcModel.builder().id("image3").tags(List.of("tag1", "tag2")).build();
        UserModel user1 = UserModel.builder().username("test").id("1").tags(List.of()).build();
        UserModel user2 = UserModel.builder().id("1").username("test").tags(List.of("tag1", "tag2")).build();

        userTable.putItem(user1);
        imageTable.putItem(image1);
        imageTable.putItem(image2);
        imageTable.putItem(image3);

        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));

        EntityExchangeResult<byte[]> result1Bytes = testClient.put()
                .uri("/api/image/tag/tag1")
                .body(BodyInserters.fromValue(new TagHandler.UpdateTagImageSrcsRequest(List.of("image1",
                                                                                               "image2",
                                                                                               "image3"), "tag1")))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .returnResult();
        // Test for duplication
        testClient.put()
                .uri("/api/image/tag/tag2")
                .body(BodyInserters.fromValue(new TagHandler.UpdateTagImageSrcsRequest(List.of("image1",
                                                                                               "image3"), "tag2")))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .returnResult();
        EntityExchangeResult<byte[]> result2Bytes = testClient.put()
                .uri("/api/image/tag/tag2")
                .body(BodyInserters.fromValue(new TagHandler.UpdateTagImageSrcsRequest(List.of("image1",
                                                                                               "image3"), "tag2")))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .returnResult();


        List<ImageSrcModel> result1 = mapper.readValue(result1Bytes.getResponseBody(), new TypeReference<List<ImageSrcModel>>() {
        });
        List<ImageSrcModel> result2 = mapper.readValue(result2Bytes.getResponseBody(), new TypeReference<List<ImageSrcModel>>() {
        });

        UserModel userResult = userTable.getItem(Key.builder().partitionValue("test").build());
        TagModel tag1Result = tagService.getTag("tag1", "1").block();
        TagModel tag2Result = tagService.getTag("tag2", "1").block();
        assertTrue(new ImageSrcMatcher(image1Updated).matches(result2.get(0)));
        assertTrue(new ImageSrcMatcher(image2Updated).matches(result1.get(1)));
        assertTrue(new ImageSrcMatcher(image3Updated).matches(result2.get(1)));
        assertTrue(new ImageSrcMatcher(image1Updated).matches(imageTable.getItem(image1)));
        assertTrue(new ImageSrcMatcher(image2Updated).matches(imageTable.getItem(image2)));
        assertTrue(new ImageSrcMatcher(image3Updated).matches(imageTable.getItem(image3)));
        assertTrue(new UserMatcher(user2).matches(userResult));
        assertTrue(new TagMatcher(tag1After).matches(tag1Result));
        assertTrue(new TagMatcher(tag2After).matches(tag2Result));
        tagTable.deleteItem(tag1Result);
        tagTable.deleteItem(tag2Result);
        userTable.deleteItem(userResult);
        imageTable.deleteItem(result2.get(0));
        imageTable.deleteItem(result1.get(1));
        imageTable.deleteItem(result2.get(1));
    }


    @Test
    void testUpdateImageSrcTags() throws IOException {
        TagModel tag1After =
                TagModel.builder().name("tag1").id("1").userId("1").imageIds(List.of("image1")).imageRankingIds(List.of()).build();
        TagModel tag2After =
                TagModel.builder().name("tag2").id("2").userId("1").imageIds(List.of("image1")).imageRankingIds(List.of()).build();
        TagModel tag3After =
                TagModel.builder().name("tag3").id("3").userId("1").imageIds(List.of("image1")).imageRankingIds(List.of()).build();

        ImageSrcModel imageBefore = ImageSrcModel.builder().id("image1").tags(List.of()).build();

        ImageSrcModel imageAfter1 = ImageSrcModel.builder().id("image1").tags(List.of("tag1", "tag2")).build();
        ImageSrcModel imageAfter2 = ImageSrcModel.builder().id("image1").tags(List.of("tag1", "tag2", "tag3")).build();
        UserModel user1 = UserModel.builder().username("test").id("1").tags(List.of()).build();
        UserModel user2 = UserModel.builder().id("1").username("test").tags(List.of("tag1", "tag2", "tag3")).build();


        userTable.putItem(user1);
        imageTable.putItem(imageBefore);

        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));

        EntityExchangeResult<byte[]> result1Bytes = testClient.put()
                .uri("/api/tag/image/image1")
                .body(BodyInserters.fromValue(new TagHandler.UpdateImageSrcTagsRequest("image1",
                                                                                       List.of("tag1",
                                                                                               "tag2"))))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .returnResult();

        EntityExchangeResult<byte[]> result2Bytes = testClient.put()
                .uri("/api/tag/image/image1")
                .body(BodyInserters.fromValue(new TagHandler.UpdateImageSrcTagsRequest("image1",
                                                                                       List.of("tag1",
                                                                                               "tag2",
                                                                                               "tag3"))))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .returnResult();

        ImageSrcModel result1 = mapper.readValue(result1Bytes.getResponseBody(), ImageSrcModel.class);
        ImageSrcModel result2 = mapper.readValue(result2Bytes.getResponseBody(), ImageSrcModel.class);
        UserModel userResult = userTable.getItem(Key.builder().partitionValue("test").build());
        TagModel tag1Result = tagService.getTag("tag1", "1").block();
        TagModel tag2Result = tagService.getTag("tag2", "1").block();
        TagModel tag3Result = tagService.getTag("tag3", "1").block();
        assertTrue(new ImageSrcMatcher(imageAfter1).matches(result1));
        assertTrue(new ImageSrcMatcher(imageAfter2).matches(result2));
        assertTrue(new UserMatcher(user2).matches(userResult));
        assertTrue(new TagMatcher(tag1After).matches(tag1Result));
        assertTrue(new TagMatcher(tag2After).matches(tag2Result));
        assertTrue(new TagMatcher(tag3After).matches(tag3Result));

        tagTable.deleteItem(tag1Result);
        tagTable.deleteItem(tag2Result);
        tagTable.deleteItem(tag3Result);
        userTable.deleteItem(userResult);
        imageTable.deleteItem(result1);
        imageTable.deleteItem(result2);
    }

    @Test
    void testDeleteImageSrcTags_BaseCase() throws IOException {
        TagModel tag1 = TagModel.builder().name("tag1").id("1").userId("1")
                .imageIds(List.of("image1")).imageRankingIds(List.of()).build();
        TagModel tag2 = TagModel.builder().name("tag2").id("2").userId("1")
                .imageIds(List.of("image1", "image2")).imageRankingIds(List.of()).build();
        TagModel tag3 = TagModel.builder().name("tag3").id("3").userId("1")
                .imageIds(List.of("image1", "image2")).imageRankingIds(List.of()).build();
        TagModel tag2After = TagModel.builder().name("tag2").id("2").userId("1")
                .imageIds(List.of("image1", "image2")).imageRankingIds(List.of()).build();
        TagModel tag3After = TagModel.builder().name("tag3").id("3").userId("1")
                .imageIds(List.of("image2")).imageRankingIds(List.of()).build();

        ImageSrcModel image1Before = ImageSrcModel.builder().id("image1").tags(List.of("tag1", "tag2", "tag3")).build();
        ImageSrcModel image2Before = ImageSrcModel.builder().id("image2").tags(List.of("tag2", "tag3")).build();
        ImageSrcModel image1After = ImageSrcModel.builder().id("image1").tags(List.of("tag2")).build();
        ImageSrcModel image2After = ImageSrcModel.builder().id("image2").tags(List.of("tag2", "tag3")).build();

        UserModel user1 = UserModel.builder().username("test").id("1").tags(List.of("tag1", "tag2", "tag3")).build();
        UserModel user2 = UserModel.builder().id("1").username("test").tags(List.of("tag2", "tag3")).build();

        tagTable.putItem(tag1);
        tagTable.putItem(tag2);
        tagTable.putItem(tag3);
        userTable.putItem(user1);
        imageTable.putItem(image1Before);
        imageTable.putItem(image2Before);

        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));

        EntityExchangeResult<byte[]> resultBytes =testClient.method(HttpMethod.DELETE)
                .uri("/api/image/tag/")
                .body(BodyInserters.fromValue(new TagHandler.DeleteImageSrcTagsRequest("image1",
                                                                                       List.of("tag1", "tag3"))))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .returnResult();

        ImageSrcModel result1 = mapper.readValue(resultBytes.getResponseBody(), ImageSrcModel.class);
        ImageSrcModel result2 = imageTable.getItem(image2Before);
        UserModel userResult = userTable.getItem(Key.builder().partitionValue("test").build());
        TagModel tag1Result = tagService.getTag("tag1", "1").block();
        TagModel tag2Result = tagService.getTag("tag2", "1").block();
        TagModel tag3Result = tagService.getTag("tag3", "1").block();
        assertTrue(new ImageSrcMatcher(image1After).matches(result1));
        assertTrue(new ImageSrcMatcher(image2After).matches(result2));
        assertTrue(new UserMatcher(user2).matches(userResult));
        assertNull(tag1Result);
        assertTrue(new TagMatcher(tag2After).matches(tag2Result));
        assertTrue(new TagMatcher(tag3After).matches(tag3Result));

        tagTable.deleteItem(tag2Result);
        tagTable.deleteItem(tag3Result);
        userTable.deleteItem(userResult);
        imageTable.deleteItem(result1);
        imageTable.deleteItem(result2);
    }

    @Test
    void testDeleteTagImageSrcs_BaseCase() throws IOException {
        TagModel tag1Before = TagModel.builder().name("tag1").id("1").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tag2Before = TagModel.builder().name("tag2").id("2").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tag3Before = TagModel.builder().name("tag3").id("3").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tag1After = TagModel.builder().name("tag1").id("1").userId("1")
                .imageIds(List.of("image2")).imageRankingIds(List.of()).build();
        TagModel tag2After = TagModel.builder().name("tag2").id("2").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();
        TagModel tag3After = TagModel.builder().name("tag3").id("3").userId("1")
                .imageIds(List.of("image1", "image2", "image3")).imageRankingIds(List.of()).build();

        ImageSrcModel image1 = ImageSrcModel.builder().id("image1").tags(List.of("tag1", "tag2", "tag3")).build();
        ImageSrcModel image2 = ImageSrcModel.builder().id("image2").tags(List.of("tag1", "tag2", "tag3")).build();
        ImageSrcModel image3 = ImageSrcModel.builder().id("image3").tags(List.of("tag1", "tag2", "tag3")).build();
        ImageSrcModel image1After = ImageSrcModel.builder().id("image1").tags(List.of("tag2", "tag3")).build();
        ImageSrcModel image2After = ImageSrcModel.builder().id("image2").tags(List.of("tag1", "tag2", "tag3")).build();
        ImageSrcModel image3After = ImageSrcModel.builder().id("image3").tags(List.of("tag2", "tag3")).build();

        UserModel user1 = UserModel.builder().username("test").id("1").tags(List.of("tag1", "tag2", "tag3")).build();
        UserModel user2 = UserModel.builder().id("1").username("test").tags(List.of("tag1", "tag2", "tag3")).build();

        tagTable.putItem(tag1Before);
        tagTable.putItem(tag2Before);
        tagTable.putItem(tag3Before);
        userTable.putItem(user1);
        imageTable.putItem(image1);
        imageTable.putItem(image2);
        imageTable.putItem(image3);

        when(jwtManager.getUserIdentity(any(ServerRequest.class))).thenReturn(new JWTManager.UserIdentity("test", "1"));

        EntityExchangeResult<byte[]> result1Bytes = testClient.method(HttpMethod.DELETE)
                .uri("/api/tag/image")
                .body(BodyInserters.fromValue(new TagHandler.DeleteTagImageSrcsRequest(List.of("image1", "image3"),
                                                                                       "tag1")))
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody()
                .returnResult();

        List<ImageSrcModel> result1 = mapper.readValue(result1Bytes.getResponseBody(), new TypeReference<List<ImageSrcModel>>() {
        });
        UserModel userResult = userTable.getItem(Key.builder().partitionValue("test").build());
        TagModel tag1Result = tagService.getTag("tag1", "1").block();
        TagModel tag2Result = tagService.getTag("tag2", "1").block();
        TagModel tag3Result = tagService.getTag("tag3", "1").block();
        assertTrue(new ImageSrcMatcher(image1After).matches(result1.get(0)));
        assertTrue(new ImageSrcMatcher(image2After).matches(imageTable.getItem(image2)));
        assertTrue(new ImageSrcMatcher(image3After).matches(result1.get(1)));
        assertTrue(new UserMatcher(user2).matches(userResult));
        assertTrue(new TagMatcher(tag1After).matches(tag1Result));
        assertTrue(new TagMatcher(tag2After).matches(tag2Result));
        assertTrue(new TagMatcher(tag3After).matches(tag3Result));

        tagTable.deleteItem(tag1Result);
        tagTable.deleteItem(tag2Result);
        tagTable.deleteItem(tag3Result);
        userTable.deleteItem(userResult);
        imageTable.deleteItem(image1);
        imageTable.deleteItem(image2);
        imageTable.deleteItem(image3);
    }
}