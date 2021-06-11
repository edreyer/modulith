
create schema if not exists public;

-- Required for Axon's JpaTokenStore
create table if not exists token_entry
(
    processor_name varchar(255)
        constraint token_entry_pk
            primary key,
    segment int,
    token text,
    token_type varchar(255),
    timestamp varchar(255),
    owner varchar(255)
);

