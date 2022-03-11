package com.scryer.endpoint.service.tag;

import com.scryer.endpoint.service.HasTags;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConsolidateUtil {

	public static <T extends HasTags> List<String> getCombinedTags(final HasTags item, final Collection<String> tags) {
		Set<String> newTags = new HashSet<>(tags.size() + item.getTags().size());
		newTags.addAll(tags);
		newTags.addAll(item.getTags());

		return List.copyOf(newTags);
	}

	public static <T extends HasTags> List<String> getSubtractedTags(final HasTags item,
			final Collection<String> tags) {
		Set<String> newTags = new HashSet<>(tags.size() + item.getTags().size());
		newTags.addAll(item.getTags());
		newTags.removeAll(tags);

		return List.copyOf(newTags);
	}

}
