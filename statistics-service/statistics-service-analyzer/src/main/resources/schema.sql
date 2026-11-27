CREATE TABLE IF NOT EXISTS user_event_interactions (
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    max_weight DOUBLE PRECISION NOT NULL,
    last_interaction_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, event_id),
    CONSTRAINT user_event_weight_check CHECK (max_weight >= 0 AND max_weight <= 1)
);

CREATE INDEX IF NOT EXISTS user_interaction_recent_idx
    ON user_event_interactions (user_id, last_interaction_at DESC);

CREATE INDEX IF NOT EXISTS user_interaction_event_idx
    ON user_event_interactions (event_id);

CREATE TABLE IF NOT EXISTS event_similarities (
    event_a BIGINT NOT NULL,
    event_b BIGINT NOT NULL,
    score DOUBLE PRECISION NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_a, event_b),
    CONSTRAINT event_pair_order_check CHECK (event_a < event_b),
    CONSTRAINT similarity_score_check CHECK (score >= 0 AND score <= 1)
);

-- Offset is monotonic for one event pair: Collector/Aggregator key the output by eventA.
-- This also migrates databases created by earlier stage-3 builds.
ALTER TABLE event_similarities
    ADD COLUMN IF NOT EXISTS applied_offset BIGINT NOT NULL DEFAULT -1;

CREATE INDEX IF NOT EXISTS event_similarity_b_idx
    ON event_similarities (event_b);
