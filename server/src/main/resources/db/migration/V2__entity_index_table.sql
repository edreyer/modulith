create table index_entity
(
    id varchar(36)
        constraint index_entity_pk
            primary key,
    aggregate_name varchar(100) not null,
    key varchar(500) not null
);

create unique index index_entity_aggregate_name_key_uindex
    on index_entity (aggregate_name, key);

