package com.lexho.user.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionClassesTest {

    @Test
    void customExceptionsExposeExpectedMessagesAndRetryAfter() {
        assertThat(new BadRequestException("bad request").getMessage()).isEqualTo("bad request");
        assertThat(new ConflictException("conflict").getMessage()).isEqualTo("conflict");
        assertThat(new ForbiddenException("forbidden").getMessage()).isEqualTo("forbidden");
        assertThat(new ResourceNotFoundException("missing").getMessage()).isEqualTo("missing");
        assertThat(new UserAlreadyExistsException("exists").getMessage()).isEqualTo("exists");
        assertThat(new UserNotFoundException(22L).getMessage()).contains("22");
        assertThat(new InvalidRoleException("manager").getMessage()).contains("Invalid role: manager");

        TooManyRequestsException exception = new TooManyRequestsException("slow down", 30L);
        assertThat(exception.getMessage()).isEqualTo("slow down");
        assertThat(exception.getRetryAfterSeconds()).isEqualTo(30L);
    }
}
