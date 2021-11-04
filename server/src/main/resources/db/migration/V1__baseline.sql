
create schema if not exists public;

create table if not exists users
(
    id varchar(255) constraint users_pk primary key,
    version int not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,

    msisdn varchar(20) not null,
    email varchar(255) not null,
    encrypted_password varchar(255) not null,
    active boolean default true
);

create unique index if not exists users_msisdn_index
    on users (msisdn);

create unique index if not exists users_email_index
    on users (email);

create table if not exists user_roles (
    user_id varchar(255) not null
        constraint fk_user_roles_to_roles
            references users(id),
    role varchar(50) not null
)

