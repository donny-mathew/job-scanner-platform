CREATE TABLE scoring_profiles (
    id           UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id    UUID        NOT NULL,
    profile_text TEXT        NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT pk_scoring_profiles PRIMARY KEY (id),
    CONSTRAINT uq_scoring_profile_per_tenant UNIQUE (tenant_id)
);

CREATE INDEX idx_scoring_profiles_tenant_id ON scoring_profiles(tenant_id);
