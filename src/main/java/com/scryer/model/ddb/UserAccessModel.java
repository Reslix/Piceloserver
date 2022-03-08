package com.scryer.model.ddb;

import com.scryer.model.ddb.converters.AuthoritiesConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.util.Collection;

@Getter
@Builder
@ToString
@AllArgsConstructor
@DynamoDbImmutable(builder = UserAccessModel.UserAccessModelBuilder.class)
public class UserAccessModel implements DynamoDBTableModel, UserDetails, HasId {
    private final String id;
    private final String username;
    private final String email;
    private final String password;
    private final boolean accountLoggedIn;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    @DynamoDbSecondaryPartitionKey(indexNames = {"userId_index"})
    public String getId() {
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
        return this.password;
    }

    @DynamoDbPartitionKey
    @Override
    public String getUsername() {
        return this.username;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
