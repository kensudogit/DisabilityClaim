package com.disabilityclaim.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("disability-claim-test-secret-32bytes!!", 60_000L);
    }

    @Test
    void generateAndParseRoundTrip() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generate(userId, "admin", List.of("ADMIN", "VIEWER"));

        Claims claims = jwtService.parse(token);
        assertThat(claims.getSubject()).isEqualTo("admin");
        assertThat(claims.get("uid", String.class)).isEqualTo(userId.toString());
        assertThat(claims.get("roles", List.class)).containsExactly("ADMIN", "VIEWER");
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void shortSecretIsPaddedToHmacKeyLength() {
        JwtService shortSecret = new JwtService("short", 60_000L);
        String token = shortSecret.generate(UUID.randomUUID(), "u", List.of("VIEWER"));
        assertThat(shortSecret.parse(token).getSubject()).isEqualTo("u");
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expired = new JwtService("disability-claim-test-secret-32bytes!!", 1L);
        String token = expired.generate(UUID.randomUUID(), "admin", List.of("ADMIN"));
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThatThrownBy(() -> expired.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generate(UUID.randomUUID(), "admin", List.of("ADMIN"));
        String tampered = token.substring(0, token.length() - 4) + "xxxx";
        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(RuntimeException.class);
    }

    @Test
    void tokenSignedWithDifferentKeyIsRejected() {
        String token = jwtService.generate(UUID.randomUUID(), "admin", List.of("ADMIN"));
        SecretKey other = Keys.hmacShaKeyFor("another-secret-key-32-bytes-min!!".getBytes(StandardCharsets.UTF_8));
        assertThatThrownBy(() -> Jwts.parser().verifyWith(other).build().parseSignedClaims(token))
                .isInstanceOf(RuntimeException.class);
    }
}
