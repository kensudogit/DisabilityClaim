package com.disabilityclaim.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResponseStatus() {
        ProblemDetail detail = handler.handleResponseStatus(
                new ResponseStatusException(HttpStatus.NOT_FOUND, "batch not found"));
        assertThat(detail.getStatus()).isEqualTo(404);
        assertThat(detail.getDetail()).isEqualTo("batch not found");
        assertThat(detail.getTitle()).isEqualTo("Request failed");
    }

    @Test
    void handleBadCredentials() {
        ProblemDetail detail = handler.handleBadCredentials(new BadCredentialsException("bad"));
        assertThat(detail.getStatus()).isEqualTo(401);
        assertThat(detail.getDetail()).isEqualTo("Invalid credentials");
    }

    @Test
    void handleIllegalState() {
        ProblemDetail detail = handler.handleIllegalState(new IllegalStateException("公式I/F仕様未提供"));
        assertThat(detail.getStatus()).isEqualTo(422);
        assertThat(detail.getDetail()).contains("公式I/F仕様未提供");
        assertThat(detail.getTitle()).isEqualTo("Illegal state");
    }

    @Test
    void handleGeneric() {
        ProblemDetail detail = handler.handleGeneric(new RuntimeException("boom"));
        assertThat(detail.getStatus()).isEqualTo(500);
        assertThat(detail.getDetail()).isEqualTo("Unexpected error");
    }
}
