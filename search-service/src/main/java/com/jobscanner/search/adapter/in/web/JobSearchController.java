package com.jobscanner.search.adapter.in.web;

import com.jobscanner.search.adapter.in.web.dto.JobSearchResponse;
import com.jobscanner.search.domain.port.in.GetJobUseCase;
import com.jobscanner.search.domain.port.in.SearchJobUseCase;
import com.jobscanner.search.domain.value.SearchQuery;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobSearchController {

    private final SearchJobUseCase searchJobUseCase;
    private final GetJobUseCase getJobUseCase;

    public JobSearchController(SearchJobUseCase searchJobUseCase, GetJobUseCase getJobUseCase) {
        this.searchJobUseCase = searchJobUseCase;
        this.getJobUseCase = getJobUseCase;
    }

    @GetMapping("/search")
    public List<JobSearchResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) Integer minScore,
            @RequestParam(defaultValue = "score") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SearchQuery query = SearchQuery.of(q, location, source, minScore, sortBy, page, size);
        return searchJobUseCase.search(query).stream()
                .map(JobSearchResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobSearchResponse> getById(@PathVariable UUID id) {
        return getJobUseCase.getById(id)
                .map(doc -> ResponseEntity.ok(JobSearchResponse.from(doc)))
                .orElse(ResponseEntity.notFound().build());
    }
}
