package com.disabilityclaim.web;

import com.disabilityclaim.domain.entity.Role;
import com.disabilityclaim.domain.entity.UserAccount;
import com.disabilityclaim.repository.UserAccountRepository;
import com.disabilityclaim.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;
    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private AuthController controller;

    @Test
    void loginReturnsAccessTokenAndRoles() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("admin");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        UUID userId = UUID.randomUUID();
        Role admin = Role.builder().id(UUID.randomUUID()).code("ADMIN").name("管理者").build();
        UserAccount user = UserAccount.builder()
                .id(userId)
                .username("admin")
                .passwordHash("x")
                .displayName("管理者")
                .roles(Set.of(admin))
                .build();
        when(userAccountRepository.findByUsernameWithRoles("admin")).thenReturn(Optional.of(user));
        when(jwtService.generate(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.eq("admin"), any()))
                .thenReturn("jwt-token");

        Map<String, Object> result = controller.login(new AuthController.LoginRequest("admin", "password123"));

        assertThat(result.get("accessToken")).isEqualTo("jwt-token");
        assertThat(result.get("tokenType")).isEqualTo("Bearer");
        assertThat(result.get("username")).isEqualTo("admin");
        @SuppressWarnings("unchecked")
        java.util.List<String> roles = (java.util.List<String>) result.get("roles");
        assertThat(roles).containsExactly("ADMIN");
    }
}
