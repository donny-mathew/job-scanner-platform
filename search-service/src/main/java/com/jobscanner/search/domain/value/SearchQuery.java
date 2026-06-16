package com.jobscanner.search.domain.value;

public record SearchQuery(
        String text,
        String location,
        String source,
        Integer minScore,
        SortField sortBy,
        int page,
        int size
) {
    public enum SortField { SCORE, DISCOVERED_AT }

    public static SearchQuery of(String text, String location, String source,
                                  Integer minScore, String sortBy, int page, int size) {
        SortField sort = "discoveredAt".equalsIgnoreCase(sortBy) ? SortField.DISCOVERED_AT : SortField.SCORE;
        return new SearchQuery(text, location, source, minScore, sort, page, size);
    }
}
