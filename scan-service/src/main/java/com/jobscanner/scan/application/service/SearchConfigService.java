package com.jobscanner.scan.application.service;

import com.jobscanner.scan.adapter.out.security.TenantContext;
import com.jobscanner.scan.domain.exception.SearchConfigNotFoundException;
import com.jobscanner.scan.domain.model.SearchConfig;
import com.jobscanner.scan.domain.port.in.SearchConfigUseCase;
import com.jobscanner.scan.domain.port.out.SearchConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SearchConfigService implements SearchConfigUseCase {

    private final SearchConfigRepository repository;

    public SearchConfigService(SearchConfigRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public SearchConfig create(CreateCommand cmd) {
        SearchConfig config = new SearchConfig(
                UUID.randomUUID(),
                TenantContext.requireTenantId(),
                cmd.keywords(),
                cmd.location(),
                cmd.filtersJson() != null ? cmd.filtersJson() : "{}",
                cmd.cronExpression(),
                cmd.enabled(),
                OffsetDateTime.now()
        );
        return repository.save(config);
    }

    @Override
    @Transactional
    public SearchConfig update(UpdateCommand cmd) {
        SearchConfig existing = repository.findById(cmd.id())
                .orElseThrow(() -> new SearchConfigNotFoundException(cmd.id()));
        SearchConfig updated = new SearchConfig(
                existing.id(),
                existing.tenantId(),
                cmd.keywords(),
                cmd.location(),
                cmd.filtersJson() != null ? cmd.filtersJson() : "{}",
                cmd.cronExpression(),
                cmd.enabled(),
                existing.createdAt()
        );
        return repository.save(updated);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        repository.findById(id).orElseThrow(() -> new SearchConfigNotFoundException(id));
        repository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchConfig getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new SearchConfigNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchConfig> listForCurrentTenant() {
        return repository.findAllForCurrentTenant();
    }
}
