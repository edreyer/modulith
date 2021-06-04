create table end_users
(
    id varchar(36)
        constraint end_users_pk
            primary key,
    msisdn varchar(50) not null,
    email varchar(200) not null,
    firstName varchar(50) not null,
    lastName varchar(100) not null
);

create unique index end_users_msisdn_uindex
    on end_users (msisdn);

