package com.scryer.util;

import com.scryer.model.ddb.TagModel;
import org.mockito.ArgumentMatcher;

import java.util.Objects;

public class TagMatcher implements ArgumentMatcher<TagModel> {
    private final TagModel left;

    public TagMatcher(final TagModel left) {
        this.left = left;
    }

    @Override
    public boolean matches(final TagModel right) {
        return right != null &&
               Objects.equals(left.getName(), right.getName()) &&
               Objects.equals(left.getImageIds(), right.getImageIds()) &&
               Objects.equals(left.getImageRankingIds(), right.getImageRankingIds()) &&
               Objects.equals(left.getUserId(), right.getUserId());
    }
}
