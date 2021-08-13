create table if not exists index_entity
(
    id uuid
        constraint index_entity_pk
            primary key,
    aggregate_name varchar(100) not null,
    key varchar(500) not null
);

drop index if exists index_entity_aggregate_name_key_uindex;
create unique index index_entity_aggregate_name_key_uindex
    on index_entity (aggregate_name, key);

