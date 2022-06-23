
create table if not exists work_orders
(
    id text constraint work_order_pk primary key,
    version int not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    deleted_at timestamp,

    status text not null,
    service text not null,
    notes text,

    start_time timestamp,
    complete_time timestamp,
    cancel_time timestamp
);

