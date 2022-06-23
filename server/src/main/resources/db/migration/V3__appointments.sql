
create table if not exists appointments
(
    id text constraint appointments_pk primary key,
    version int not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,

    work_order_id text not null,

    user_id text not null,
    scheduled_time timestamp,
    duration int,
    status text not null,

    complete_time timestamp,
    cancel_time timestamp

);

create index if not exists appointments_user_id_index
    on appointments (user_id);

create unique index if not exists appointments_start_time_index
    on appointments (scheduled_time);

create index if not exists appointments_work_orders_id_index
    on appointments (work_order_id);

alter table appointments
    drop constraint if exists fk_appointments_work_orders;

alter table appointments
    add constraint fk_appointments_work_orders
        foreign key (work_order_id) references work_orders;
