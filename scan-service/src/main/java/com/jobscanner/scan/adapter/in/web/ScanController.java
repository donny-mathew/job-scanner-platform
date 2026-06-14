package com.jobscanner.scan.adapter.in.web;

import com.jobscanner.scan.adapter.in.web.dto.ScanRunResponse;
import com.jobscanner.scan.adapter.out.security.TenantContext;
import com.jobscanner.scan.domain.port.in.ScanUseCase;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/scans")
public class ScanController {

    private final ScanUseCase scanUseCase;

    public ScanController(ScanUseCase scanUseCase) {
        this.scanUseCase = scanUseCase;
    }

    @PostMapping("/run")
    public ScanRunResponse run() {
        int count = scanUseCase.runScan(TenantContext.requireTenantId());
        return new ScanRunResponse(count, count + " new job(s) discovered and queued for scoring.");
    }
}
