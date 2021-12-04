
create table if not exists appointments
(
    id varchar(255) constraint appointments_pk primary key,
    version int not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,

    user_id varchar(255) not null,
    start_time timestamp,
    duration int,
    status varchar(32) not null,

    complete_date timestamp,
    cancel_date timestamp

);

create index if not exists appointments_user_id_index
    on appointments (user_id);

create unique index if not exists appointments_start_time_index
    on appointments (start_time);
