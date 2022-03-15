package com.picelo.util;

import com.picelo.endpoint.service.user.User;
import org.mockito.ArgumentMatcher;

import java.util.Objects;

public class UserMatcher implements ArgumentMatcher<User> {

	private final User left;

	public UserMatcher(final User left) {
		this.left = left;
	}

	@Override
	public boolean matches(final User right) {
		return right != null && Objects.equals(left.getUsername(), right.getUsername())
				&& Objects.equals(left.getId(), right.getId())
				&& Objects.equals(left.getDisplayName(), right.getDisplayName())
				&& Objects.equals(left.getFirstName(), right.getFirstName())
				&& Objects.equals(left.getLastName(), right.getLastName())
				&& Objects.equals(left.getEmail(), right.getEmail())
				&& Objects.equals(left.getRootFolderId(), right.getRootFolderId())
				&& Objects.equals(left.getCreateDate(), right.getCreateDate());
	}

}
