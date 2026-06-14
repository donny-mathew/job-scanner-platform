package com.jobscanner.auth.domain.port.in;

import java.util.UUID;

public interface SignUpUseCase {

    record SignUpCommand(String email, String password, String tenantName, UUID tenantId) {}

    record SignUpResult(UUID userId, UUID tenantId, String tenantName, String token) {}

    SignUpResult signUp(SignUpCommand command);
}
