
create schema if not exists public;

create table if not exists users
(
    id varchar(255) not null constraint users_pk primary key
);

