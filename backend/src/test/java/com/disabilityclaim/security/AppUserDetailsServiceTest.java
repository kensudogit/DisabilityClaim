package com.disabilityclaim.security;

import com.disabilityclaim.domain.entity.Role;
import com.disabilityclaim.domain.entity.UserAccount;
import com.disabilityclaim.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private AppUserDetailsService service;

    @Test
    void loadUserByUsernameMapsRoles() {
        Role role = Role.builder().id(UUID.randomUUID()).code("ADMIN").name("管理者").build();
        UserAccount account = UserAccount.builder()
                .id(UUID.randomUUID())
                .username("admin")
                .passwordHash("hash")
                .displayName("管理者")
                .enabled(true)
                .roles(Set.of(role))
                .build();
        when(userAccountRepository.findByUsernameWithRoles("admin")).thenReturn(Optional.of(account));

        UserDetails details = service.loadUserByUsername("admin");
        assertThat(details.getUsername()).isEqualTo("admin");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsernameNotFound() {
        when(userAccountRepository.findByUsernameWithRoles("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void disabledUserIsNotEnabled() {
        UserAccount account = UserAccount.builder()
                .id(UUID.randomUUID())
                .username("disabled")
                .passwordHash("hash")
                .displayName("x")
                .enabled(false)
                .roles(Set.of())
                .build();
        when(userAccountRepository.findByUsernameWithRoles("disabled")).thenReturn(Optional.of(account));
        assertThat(service.loadUserByUsername("disabled").isEnabled()).isFalse();
    }
}
