package com.jobscanner.scan.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface SearchConfigJpaRepository extends JpaRepository<SearchConfigJpaEntity, UUID> {
    List<SearchConfigJpaEntity> findAllByTenantId(UUID tenantId);
    List<SearchConfigJpaEntity> findAllByEnabledTrue();
}
