
create schema if not exists public;

CREATE TABLE IF NOT EXISTS token_entry
(
    processor_name VARCHAR(255) NOT NULL,
    segment INTEGER NOT NULL,
    owner VARCHAR(255),
    timestamp VARCHAR(255) NOT NULL,
    token oid,
    token_type VARCHAR(255),
    PRIMARY KEY (processor_name, segment)
);

-- Required for Axon's JpaTokenStore
CREATE TABLE IF NOT EXISTS association_value_entry
(
    id SERIAL,
    association_key VARCHAR(255) NOT NULL,
    association_value VARCHAR(255),
    saga_id VARCHAR(255) NOT NULL,
    saga_type VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS association_value_entry_fields_idx
    on association_value_entry (saga_type, association_key, association_value);

CREATE INDEX IF NOT EXISTS association_value_entry_saga_id_saga_type_idx
    on association_value_entry (saga_id, saga_type);

CREATE TABLE IF NOT EXISTS saga_entry
(
    saga_id VARCHAR(255) NOT NULL,
    revision VARCHAR(255),
    saga_type VARCHAR(255),
    serialized_saga oid,
    PRIMARY KEY (saga_id)
);
