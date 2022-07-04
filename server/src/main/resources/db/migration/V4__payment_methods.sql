
create table if not exists payment_methods
(
    id                       text
        constraint payment_methods_pk primary key,
    version                  int       not null,
    created_at               timestamp not null,
    updated_at               timestamp not null,
    deleted_at               timestamp,

    user_id                  text      not null,
    stripe_payment_method_id text      not null,
    last_four                text      not null
);

create index if not exists payment_methods_user_id_index
    on payment_methods (user_id);
