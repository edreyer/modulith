
create table if not exists payments
(
    id                       text
        constraint payments_pk primary key,
    version                  int       not null,
    created_at               timestamp not null,
    updated_at               timestamp not null,
    deleted_at               timestamp,

    user_id                  text      not null,
    payment_method_id        text      not null,
    amount                   bigint    not null
);

create index if not exists payments_user_id_index
    on payments (user_id);

alter table appointments
    add column if not exists payment_id text;

alter table work_orders
    add column if not exists payment_time timestamp;
