package com.jobscanner.auth.domain.port.in;

import java.util.UUID;

public interface LoginUseCase {

    record LoginCommand(String email, String password, UUID tenantId) {}

    record LoginResult(String token, UUID tenantId, UUID userId) {}

    LoginResult login(LoginCommand command);
}
