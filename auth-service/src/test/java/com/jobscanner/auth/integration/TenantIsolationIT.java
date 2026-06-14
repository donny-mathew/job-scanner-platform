package com.jobscanner.auth.integration;

import com.jobscanner.auth.adapter.out.security.TenantContext;
import com.jobscanner.auth.domain.model.User;
import com.jobscanner.auth.domain.port.in.SignUpUseCase;
import com.jobscanner.auth.domain.port.in.SignUpUseCase.SignUpCommand;
import com.jobscanner.auth.domain.port.out.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that tenant data isolation holds at the repository layer.
 * Two tenants are created; each tenant's user is invisible to the other's context.
 */
@SpringBootTest
@Testcontainers
class TenantIsolationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("authdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", () -> "localhost");
        registry.add("spring.data.redis.port", () -> "6379");
        registry.add("app.jwt.secret", () -> "integration-test-secret-min-32-chars!!");
    }

    @Autowired SignUpUseCase signUpUseCase;
    @Autowired UserRepository userRepository;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void tenantA_cannotSeeUserFromTenantB() {
        // Create Tenant A with user Alice
        SignUpUseCase.SignUpResult resultA = signUpUseCase.signUp(
                new SignUpCommand("alice@alpha.com", "password123", "alpha-" + UUID.randomUUID(), null));
        UUID tenantAId = resultA.tenantId();

        // Create Tenant B with user Bob
        SignUpUseCase.SignUpResult resultB = signUpUseCase.signUp(
                new SignUpCommand("bob@beta.com", "password123", "beta-" + UUID.randomUUID(), null));
        UUID tenantBId = resultB.tenantId();

        // Scope to Tenant A — Bob must be invisible
        TenantContext.set(tenantAId);
        Optional<User> bobFromTenantA = userRepository.findByEmail("bob@beta.com");
        assertThat(bobFromTenantA).isEmpty();

        // Alice must be visible
        Optional<User> aliceFromTenantA = userRepository.findByEmail("alice@alpha.com");
        assertThat(aliceFromTenantA).isPresent();
        assertThat(aliceFromTenantA.get().tenantId()).isEqualTo(tenantAId);
        TenantContext.clear();

        // Scope to Tenant B — Alice must be invisible
        TenantContext.set(tenantBId);
        Optional<User> aliceFromTenantB = userRepository.findByEmail("alice@alpha.com");
        assertThat(aliceFromTenantB).isEmpty();

        // Bob must be visible
        Optional<User> bobFromTenantB = userRepository.findByEmail("bob@beta.com");
        assertThat(bobFromTenantB).isPresent();
        assertThat(bobFromTenantB.get().tenantId()).isEqualTo(tenantBId);
    }

    @Test
    void sameEmailAllowedInDifferentTenants() {
        // Shared email across two tenants must not conflict
        String sharedEmail = "shared@example.com";

        SignUpUseCase.SignUpResult resultA = signUpUseCase.signUp(
                new SignUpCommand(sharedEmail, "password123", "tenant-x-" + UUID.randomUUID(), null));
        SignUpUseCase.SignUpResult resultB = signUpUseCase.signUp(
                new SignUpCommand(sharedEmail, "password123", "tenant-y-" + UUID.randomUUID(), null));

        assertThat(resultA.tenantId()).isNotEqualTo(resultB.tenantId());

        // Each tenant sees only their own user
        TenantContext.set(resultA.tenantId());
        assertThat(userRepository.findByEmail(sharedEmail)).isPresent()
                .hasValueSatisfying(u -> assertThat(u.tenantId()).isEqualTo(resultA.tenantId()));
        TenantContext.clear();

        TenantContext.set(resultB.tenantId());
        assertThat(userRepository.findByEmail(sharedEmail)).isPresent()
                .hasValueSatisfying(u -> assertThat(u.tenantId()).isEqualTo(resultB.tenantId()));
    }
}
