package com.jobscanner.auth.adapter.in.web;

import com.jobscanner.auth.adapter.in.web.dto.LoginRequest;
import com.jobscanner.auth.adapter.in.web.dto.LoginResponse;
import com.jobscanner.auth.adapter.in.web.dto.SignUpRequest;
import com.jobscanner.auth.adapter.in.web.dto.SignUpResponse;
import com.jobscanner.auth.domain.port.in.LoginUseCase;
import com.jobscanner.auth.domain.port.in.SignUpUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final SignUpUseCase signUpUseCase;
    private final LoginUseCase loginUseCase;

    public AuthController(SignUpUseCase signUpUseCase, LoginUseCase loginUseCase) {
        this.signUpUseCase = signUpUseCase;
        this.loginUseCase = loginUseCase;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public SignUpResponse signUp(@Valid @RequestBody SignUpRequest request) {
        var result = signUpUseCase.signUp(new SignUpUseCase.SignUpCommand(
                request.email(),
                request.password(),
                request.tenantName(),
                request.tenantId()
        ));
        return new SignUpResponse(result.userId(), result.tenantId(), result.tenantName(), result.token());
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        var result = loginUseCase.login(new LoginUseCase.LoginCommand(
                request.email(),
                request.password(),
                request.tenantId()
        ));
        return new LoginResponse(result.token(), result.tenantId(), result.userId());
    }
}
