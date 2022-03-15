package com.picelo.endpoint.service.tag;

import com.picelo.util.ConsolidateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class TagService {

    private final TagDBAdapter tagDBAdapter;

    @Autowired
    public TagService(final TagDBAdapter tagDBAdapter) {
        this.tagDBAdapter = tagDBAdapter;
    }

    public Mono<TagModel> getTag(final String name, final String userId) {
        return tagDBAdapter.getTag(name, userId);
    }

    public Flux<TagModel> getUserTags(final String userId) {
        return tagDBAdapter.getTags(userId);
    }

    public Mono<TagModel> addNewTag(final String name, final String userId) {
        return tagDBAdapter.getUniqueId().map(id -> TagModel.builder()
                .id(id)
                .name(name)
                .imageIds(List.of())
                .imageRankingIds(List.of())
                .userId(userId)
                .build()).flatMap(tagDBAdapter::addTag);
    }

    public Mono<TagModel> updateTagImageIds(final TagModel tag, final List<String> imageIds) {
        var ids = ConsolidateUtil.getCombined(tag.getImageIds(), imageIds);
        var newTagModel = TagModel.builder().id(tag.getId()).imageIds(List.copyOf(ids)).build();
        return tagDBAdapter.updateTag(newTagModel);
    }

    public Mono<TagModel> updateTagImageRankingIds(final TagModel tag, final List<String> rankingIds) {
        var ids = ConsolidateUtil.getCombined(tag.getImageRankingIds(), rankingIds);
        var newTagModel = TagModel.builder().id(tag.getId()).imageRankingIds(List.copyOf(ids)).build();
        return tagDBAdapter.updateTag(newTagModel);
    }

    public Mono<TagModel> deleteTagImages(final TagModel tag, final List<String> imageIds) {
        var ids = ConsolidateUtil.getSubtracted(tag.getImageIds(), imageIds);
        var newTagModel = TagModel.builder().id(tag.getId()).imageIds(List.copyOf(ids)).build();
        if (ids.size() > 0 || tag.getImageRankingIds().size() > 0) {
            return tagDBAdapter.updateTag(newTagModel);
        } else {
            return tagDBAdapter.updateTag(newTagModel).flatMap(tagDBAdapter::deleteTag);
        }
    }

    public Mono<TagModel> deleteTagImageRankings(final TagModel tag, final List<String> rankingIds) {
        var ids = ConsolidateUtil.getSubtracted(tag.getImageRankingIds(), rankingIds);
        var newTagModel = TagModel.builder().id(tag.getId()).imageRankingIds(List.copyOf(ids)).build();

        if (ids.size() > 0 || tag.getImageIds().size() > 0) {
            return tagDBAdapter.updateTag(newTagModel);
        } else {
            return tagDBAdapter.updateTag(newTagModel).flatMap(tagDBAdapter::deleteTag);
        }
    }

}
