create table if not exists user_roles
(
    user_id uuid not null
        constraint fk_user_roles_to_roles
            references user_view(id),
    role varchar(255)
);
