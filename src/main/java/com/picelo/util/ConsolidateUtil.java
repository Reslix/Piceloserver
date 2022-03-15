package com.picelo.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConsolidateUtil {

	public static List<String> getCombined(final Collection<String> current, final Collection<String> toAdd) {
		Set<String> combined = new HashSet<>();
		combined.addAll(current);
		combined.addAll(toAdd);

		return List.copyOf(combined);
	}

	public static List<String> getSubtracted(final Collection<String> current, final Collection<String> toRemove) {
		Set<String> combined = new HashSet<>();
		combined.addAll(current);
		combined.removeAll(toRemove);

		return List.copyOf(combined);
	}

}
