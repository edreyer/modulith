
create schema if not exists public;

create table if not exists users
(
    id varchar(255) constraint users_pk primary key,
    username varchar(255) not null,
    email varchar(255) not null,
    encrypted_password varchar(255) not null
);

