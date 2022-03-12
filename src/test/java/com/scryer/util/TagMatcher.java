package com.scryer.util;

import com.scryer.endpoint.service.tag.TagModel;
import org.mockito.ArgumentMatcher;

import java.util.Objects;
import java.util.Set;

public class TagMatcher implements ArgumentMatcher<TagModel> {

	private final TagModel left;

	public TagMatcher(final TagModel left) {
		this.left = left;
	}

	@Override
	public boolean matches(final TagModel right) {
		return right != null && Objects.equals(left.getName(), right.getName())
				&& Objects.equals(Set.copyOf(left.getImageIds()),Set.copyOf( right.getImageIds()))
				&& Objects.equals(Set.copyOf(left.getImageRankingIds()), Set.copyOf(right.getImageRankingIds()))
				&& Objects.equals(left.getUserId(), right.getUserId());
	}

}
