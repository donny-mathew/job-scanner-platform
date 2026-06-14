package com.jobscanner.auth.unit;

import com.jobscanner.auth.adapter.out.security.TenantContext;
import com.jobscanner.auth.application.service.AuthService;
import com.jobscanner.auth.application.service.JwtService;
import com.jobscanner.auth.domain.exception.InvalidCredentialsException;
import com.jobscanner.auth.domain.exception.TenantNotFoundException;
import com.jobscanner.auth.domain.exception.UserAlreadyExistsException;
import com.jobscanner.auth.domain.model.Tenant;
import com.jobscanner.auth.domain.model.User;
import com.jobscanner.auth.domain.port.in.LoginUseCase.LoginCommand;
import com.jobscanner.auth.domain.port.in.LoginUseCase.LoginResult;
import com.jobscanner.auth.domain.port.in.SignUpUseCase.SignUpCommand;
import com.jobscanner.auth.domain.port.in.SignUpUseCase.SignUpResult;
import com.jobscanner.auth.domain.port.out.TenantRepository;
import com.jobscanner.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock TenantRepository tenantRepository;
    @Mock UserRepository userRepository;
    @Mock JwtService jwtService;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    AuthService authService;

    UUID tenantId = UUID.randomUUID();
    Tenant tenant = new Tenant(tenantId, "acme", OffsetDateTime.now());

    @BeforeEach
    void setUp() {
        authService = new AuthService(tenantRepository, userRepository, passwordEncoder, jwtService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // --- signUp ---

    @Test
    void signUp_newTenant_createsOwnerAndReturnToken() {
        when(tenantRepository.save(any())).thenReturn(tenant);
        when(userRepository.existsByEmail("alice@acme.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        SignUpResult result = authService.signUp(
                new SignUpCommand("alice@acme.com", "password123", "acme", null));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.tenantName()).isEqualTo("acme");

        verify(tenantRepository).save(argThat(t -> t.name().equals("acme")));
        verify(userRepository).save(argThat(u -> u.role() == User.Role.OWNER));
    }

    @Test
    void signUp_existingTenant_addsMember() {
        UUID existingTenantId = tenantId;
        when(tenantRepository.findById(existingTenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.existsByEmail("bob@acme.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        authService.signUp(new SignUpCommand("bob@acme.com", "password123", null, existingTenantId));

        verify(userRepository).save(argThat(u -> u.role() == User.Role.MEMBER));
    }

    @Test
    void signUp_emailAlreadyExistsInTenant_throws() {
        when(tenantRepository.save(any())).thenReturn(tenant);
        when(userRepository.existsByEmail("alice@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.signUp(
                new SignUpCommand("alice@acme.com", "password123", "acme", null)))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void signUp_unknownTenantId_throws() {
        UUID unknownId = UUID.randomUUID();
        when(tenantRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.signUp(
                new SignUpCommand("alice@acme.com", "password123", null, unknownId)))
                .isInstanceOf(TenantNotFoundException.class);
    }

    // --- login ---

    @Test
    void login_validCredentials_returnsToken() {
        String rawPassword = "password123";
        String hash = passwordEncoder.encode(rawPassword);
        User user = new User(UUID.randomUUID(), tenantId, "alice@acme.com", hash,
                User.Role.OWNER, OffsetDateTime.now());

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResult result = authService.login(new LoginCommand("alice@acme.com", rawPassword, tenantId));

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void login_wrongPassword_throws() {
        String hash = passwordEncoder.encode("correct-password");
        User user = new User(UUID.randomUUID(), tenantId, "alice@acme.com", hash,
                User.Role.OWNER, OffsetDateTime.now());

        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmail("alice@acme.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                new LoginCommand("alice@acme.com", "wrong-password", tenantId)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownEmail_throws() {
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmail("ghost@acme.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginCommand("ghost@acme.com", "password", tenantId)))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_unknownTenant_throws() {
        UUID unknownTenant = UUID.randomUUID();
        when(tenantRepository.findById(unknownTenant)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(
                new LoginCommand("alice@acme.com", "password", unknownTenant)))
                .isInstanceOf(TenantNotFoundException.class);
    }
}
