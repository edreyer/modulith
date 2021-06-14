create table if not exists user_view
(
    user_id uuid
        constraint end_user_view_pk
            primary key,
    username    varchar(50) not null,
    email       varchar(255) not null
);

drop index if exists user_view_username_uindex;
create unique index user_view_username_uindex
    on user_view (username);

