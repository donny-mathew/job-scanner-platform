package com.jobscanner.scan.domain.port.out;

import com.jobscanner.scan.domain.value.RawJob;
import com.jobscanner.scan.domain.value.SearchCriteria;

import java.util.List;

public interface JobDataProvider {
    List<RawJob> fetchJobs(SearchCriteria criteria);
}
