package com.eventzone.security;

import com.eventzone.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts our {@link User} entity to Spring Security's {@link UserDetails} while
 * keeping the entity itself reachable, so {@link SecurityUtils#currentUser()}
 * can hand controllers the domain object rather than just an email.
 *
 * <p>The entity stores a bare role name (ATTENDEE | ORGANISER | ADMIN); the
 * "ROLE_" prefix Spring Security expects for hasRole() checks is added here.
 */
public class EventZoneUserPrincipal implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    private final transient User user;

    public EventZoneUserPrincipal(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(ROLE_PREFIX + user.getRole()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
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
