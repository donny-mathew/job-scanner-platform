package com.jobscanner.scan.domain.port.in;

import com.jobscanner.scan.domain.model.SearchConfig;

import java.util.List;
import java.util.UUID;

public interface SearchConfigUseCase {

    record CreateCommand(List<String> keywords, String location, String filtersJson,
                         String cronExpression, boolean enabled) {}

    record UpdateCommand(UUID id, List<String> keywords, String location, String filtersJson,
                         String cronExpression, boolean enabled) {}

    SearchConfig create(CreateCommand cmd);
    SearchConfig update(UpdateCommand cmd);
    void delete(UUID id);
    SearchConfig getById(UUID id);
    List<SearchConfig> listForCurrentTenant();
}
