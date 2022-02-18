package com.scryer.model.ddb;

import com.scryer.model.ddb.converters.AuthoritiesConverter;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
@DynamoDbImmutable(builder = UserSecurityModel.UserSecurityModelBuilder.class)
public class UserSecurityModel implements DynamoDBTableModel, UserDetails {
    private final Long id;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final List<? extends GrantedAuthority> authorities;

    @DynamoDbSecondaryPartitionKey(indexNames = {"userId_index"})
    public Long getId() {
        return this.id;
    }

    @DynamoDbSecondaryPartitionKey(indexNames = {"email_index"})
    public String getEmail() {
        return this.email;
    }

    @DynamoDbConvertedBy(AuthoritiesConverter.class)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.passwordHash;
    }

    @DynamoDbPartitionKey
    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
