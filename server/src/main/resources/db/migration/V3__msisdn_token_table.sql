create table if not exists msisdn_token_view
(
    id uuid not null
        constraint msisdn_tokens_pkey
            primary key,
    token varchar(10) not null,
    msisdn varchar(30) not null,
    email varchar(255),
    expires timestamp not null,
    used boolean not null
);

create index msisdn_tokens_index
    on msisdn_token_view (msisdn, token);

