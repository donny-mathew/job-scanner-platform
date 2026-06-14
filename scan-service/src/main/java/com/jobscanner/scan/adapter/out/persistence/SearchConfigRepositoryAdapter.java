package com.jobscanner.scan.adapter.out.persistence;

import com.jobscanner.scan.adapter.out.security.TenantContext;
import com.jobscanner.scan.domain.model.SearchConfig;
import com.jobscanner.scan.domain.port.out.SearchConfigRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SearchConfigRepositoryAdapter implements SearchConfigRepository {

    private final SearchConfigJpaRepository jpa;

    public SearchConfigRepositoryAdapter(SearchConfigJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public SearchConfig save(SearchConfig config) {
        return jpa.save(SearchConfigJpaEntity.fromDomain(config)).toDomain();
    }

    @Override
    public Optional<SearchConfig> findById(UUID id) {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .map(SearchConfigJpaEntity::toDomain);
    }

    @Override
    public List<SearchConfig> findAllForCurrentTenant() {
        UUID tenantId = TenantContext.requireTenantId();
        return jpa.findAllByTenantId(tenantId).stream()
                .map(SearchConfigJpaEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpa.deleteById(id);
    }

    @Override
    public List<SearchConfig> findAllEnabled() {
        // Cross-tenant — used by scheduler only, no TenantContext required
        return jpa.findAllByEnabledTrue().stream()
                .map(SearchConfigJpaEntity::toDomain)
                .toList();
    }
}
