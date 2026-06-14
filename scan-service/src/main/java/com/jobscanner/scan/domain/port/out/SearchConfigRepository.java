package com.jobscanner.scan.domain.port.out;

import com.jobscanner.scan.domain.model.SearchConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SearchConfigRepository {
    SearchConfig save(SearchConfig config);
    Optional<SearchConfig> findById(UUID id);
    List<SearchConfig> findAllForCurrentTenant();
    void deleteById(UUID id);

    /** Cross-tenant query used by the scheduler only — bypasses TenantContext. */
    List<SearchConfig> findAllEnabled();
}
