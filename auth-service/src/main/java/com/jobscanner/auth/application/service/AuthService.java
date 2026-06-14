package com.jobscanner.auth.application.service;

import com.jobscanner.auth.adapter.out.security.TenantContext;
import com.jobscanner.auth.domain.exception.InvalidCredentialsException;
import com.jobscanner.auth.domain.exception.TenantNotFoundException;
import com.jobscanner.auth.domain.exception.UserAlreadyExistsException;
import com.jobscanner.auth.domain.model.Tenant;
import com.jobscanner.auth.domain.model.User;
import com.jobscanner.auth.domain.port.in.LoginUseCase;
import com.jobscanner.auth.domain.port.in.SignUpUseCase;
import com.jobscanner.auth.domain.port.out.TenantRepository;
import com.jobscanner.auth.domain.port.out.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class AuthService implements SignUpUseCase, LoginUseCase {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(TenantRepository tenantRepository,
                       UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public SignUpResult signUp(SignUpCommand command) {
        Tenant tenant = resolveTenantForSignUp(command);

        // Scope context so UserRepository can enforce tenant isolation on the existence check
        TenantContext.set(tenant.id());
        try {
            if (userRepository.existsByEmail(command.email())) {
                throw new UserAlreadyExistsException(command.email());
            }

            User.Role role = command.tenantId() == null ? User.Role.OWNER : User.Role.MEMBER;
            User user = new User(
                    UUID.randomUUID(),
                    tenant.id(),
                    command.email(),
                    passwordEncoder.encode(command.password()),
                    role,
                    OffsetDateTime.now()
            );
            User saved = userRepository.save(user);
            String token = jwtService.generateToken(saved);
            return new SignUpResult(saved.id(), tenant.id(), tenant.name(), token);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResult login(LoginCommand command) {
        Tenant tenant = tenantRepository.findById(command.tenantId())
                .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));

        TenantContext.set(tenant.id());
        try {
            User user = userRepository.findByEmail(command.email())
                    .orElseThrow(InvalidCredentialsException::new);

            if (!passwordEncoder.matches(command.password(), user.passwordHash())) {
                throw new InvalidCredentialsException();
            }

            String token = jwtService.generateToken(user);
            return new LoginResult(token, tenant.id(), user.id());
        } finally {
            TenantContext.clear();
        }
    }

    private Tenant resolveTenantForSignUp(SignUpCommand command) {
        if (command.tenantId() != null) {
            return tenantRepository.findById(command.tenantId())
                    .orElseThrow(() -> new TenantNotFoundException(command.tenantId()));
        }
        String name = command.tenantName() != null
                ? command.tenantName()
                : command.email().split("@")[1].split("\\.")[0];

        return tenantRepository.save(new Tenant(UUID.randomUUID(), name, OffsetDateTime.now()));
    }
}
