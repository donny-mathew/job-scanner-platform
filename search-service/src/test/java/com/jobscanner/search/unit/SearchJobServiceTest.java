package com.jobscanner.search.unit;

import com.jobscanner.search.application.service.IndexJobService;
import com.jobscanner.search.application.service.SearchJobService;
import com.jobscanner.search.domain.model.JobSearchDocument;
import com.jobscanner.search.domain.port.out.JobSearchIndex;
import com.jobscanner.search.domain.value.SearchQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SearchJobServiceTest {

    @Mock JobSearchIndex searchIndex;

    @InjectMocks SearchJobService searchJobService;
    @InjectMocks IndexJobService indexJobService;

    private JobSearchDocument sampleDoc() {
        return new JobSearchDocument(UUID.randomUUID(), UUID.randomUUID(),
                "Java Engineer", "Acme", "Sydney", "https://example.com",
                "mock", OffsetDateTime.now(), 80, "Good match", "mock", OffsetDateTime.now());
    }

    @Test
    void search_delegatesToIndex() {
        SearchQuery query = SearchQuery.of(null, null, null, null, "score", 0, 20);
        JobSearchDocument doc = sampleDoc();
        when(searchIndex.search(query)).thenReturn(List.of(doc));

        List<JobSearchDocument> results = searchJobService.search(query);

        assertThat(results).containsExactly(doc);
        verify(searchIndex).search(query);
    }

    @Test
    void getById_delegatesToIndex() {
        UUID id = UUID.randomUUID();
        JobSearchDocument doc = sampleDoc();
        when(searchIndex.getById(id)).thenReturn(Optional.of(doc));

        Optional<JobSearchDocument> result = searchJobService.getById(id);

        assertThat(result).contains(doc);
    }

    @Test
    void indexJob_delegatesToIndex() {
        JobSearchDocument doc = sampleDoc();
        indexJobService.index(doc);
        verify(searchIndex).index(doc);
    }

    @Test
    void indexJob_calledTwice_callsIndexTwice_idempotencyIsInAdapter() {
        JobSearchDocument doc = sampleDoc();
        indexJobService.index(doc);
        indexJobService.index(doc);
        verify(searchIndex, times(2)).index(doc);
    }
}
