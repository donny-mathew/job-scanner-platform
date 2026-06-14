CREATE TABLE job_scores (
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id      UUID        NOT NULL,
    job_listing_id UUID        NOT NULL,
    score          SMALLINT    NOT NULL CHECK (score BETWEEN 0 AND 100),
    reasoning      TEXT        NOT NULL,
    model          VARCHAR(100) NOT NULL,
    scored_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_job_scores               PRIMARY KEY (id),
    CONSTRAINT uq_job_score_per_tenant     UNIQUE (tenant_id, job_listing_id)
);

CREATE INDEX idx_job_scores_tenant_id ON job_scores(tenant_id);
