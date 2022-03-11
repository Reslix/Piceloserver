package com.scryer.util;

import com.scryer.endpoint.service.imagesrc.ImageSrcModel;
import org.mockito.ArgumentMatcher;

import java.util.Objects;

public class ImageSrcMatcher implements ArgumentMatcher<ImageSrcModel> {

	private final ImageSrcModel left;

	public ImageSrcMatcher(final ImageSrcModel left) {
		this.left = left;
	}

	@Override
	public boolean matches(final ImageSrcModel right) {
		return right != null && Objects.equals(left.getId(), right.getId())
				&& Objects.equals(left.getName(), right.getName())
				&& Objects.equals(left.getParentFolderId(), right.getParentFolderId())
				&& Objects.equals(left.getSize(), right.getSize())
				&& Objects.equals(left.getAlternateSizes(), right.getAlternateSizes())
				&& Objects.equals(left.getSource(), right.getSource())
				&& Objects.equals(left.getType(), right.getType())
				&& Objects.equals(left.getUserId(), right.getUserId())
				&& Objects.equals(left.getTags(), right.getTags());
	}

}
