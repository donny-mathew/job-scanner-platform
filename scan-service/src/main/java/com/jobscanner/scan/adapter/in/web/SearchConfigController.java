package com.jobscanner.scan.adapter.in.web;

import com.jobscanner.scan.adapter.in.web.dto.SearchConfigRequest;
import com.jobscanner.scan.adapter.in.web.dto.SearchConfigResponse;
import com.jobscanner.scan.domain.port.in.SearchConfigUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search-configs")
public class SearchConfigController {

    private final SearchConfigUseCase useCase;

    public SearchConfigController(SearchConfigUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SearchConfigResponse create(@Valid @RequestBody SearchConfigRequest req) {
        return SearchConfigResponse.from(useCase.create(
                new SearchConfigUseCase.CreateCommand(req.keywords(), req.location(),
                        req.filtersJson(), req.cronExpression(), req.enabled())));
    }

    @GetMapping
    public List<SearchConfigResponse> list() {
        return useCase.listForCurrentTenant().stream()
                .map(SearchConfigResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SearchConfigResponse get(@PathVariable UUID id) {
        return SearchConfigResponse.from(useCase.getById(id));
    }

    @PutMapping("/{id}")
    public SearchConfigResponse update(@PathVariable UUID id,
                                       @Valid @RequestBody SearchConfigRequest req) {
        return SearchConfigResponse.from(useCase.update(
                new SearchConfigUseCase.UpdateCommand(id, req.keywords(), req.location(),
                        req.filtersJson(), req.cronExpression(), req.enabled())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        useCase.delete(id);
    }
}
