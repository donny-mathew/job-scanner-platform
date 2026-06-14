CREATE TABLE job_listings (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL,
    external_id   VARCHAR(512) NOT NULL,
    title         VARCHAR(512) NOT NULL,
    company       VARCHAR(255) NOT NULL,
    location      VARCHAR(255),
    url           TEXT         NOT NULL,
    raw_payload   JSONB        NOT NULL DEFAULT '{}',
    source        VARCHAR(100) NOT NULL,
    discovered_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_job_listings              PRIMARY KEY (id),
    CONSTRAINT uq_job_listing_per_tenant    UNIQUE (tenant_id, external_id)
);

CREATE INDEX idx_job_listings_tenant_id ON job_listings(tenant_id);
CREATE INDEX idx_job_listings_external  ON job_listings(tenant_id, external_id);
