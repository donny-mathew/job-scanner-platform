CREATE TABLE search_configs (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id       UUID         NOT NULL,
    keywords        TEXT[]       NOT NULL,
    location        VARCHAR(255),
    filters_json    JSONB        NOT NULL DEFAULT '{}',
    cron_expression VARCHAR(100),
    enabled         BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_search_configs PRIMARY KEY (id)
);

CREATE INDEX idx_search_configs_tenant_id ON search_configs(tenant_id);
CREATE INDEX idx_search_configs_enabled   ON search_configs(tenant_id, enabled);
