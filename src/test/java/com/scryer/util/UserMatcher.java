package com.scryer.util;

import com.scryer.endpoint.service.user.UserModel;
import org.mockito.ArgumentMatcher;

import java.util.Objects;

public class UserMatcher implements ArgumentMatcher<UserModel> {

	private final UserModel left;

	public UserMatcher(final UserModel left) {
		this.left = left;
	}

	@Override
	public boolean matches(final UserModel right) {
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
