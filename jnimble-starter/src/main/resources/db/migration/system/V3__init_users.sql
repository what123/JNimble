create table if not exists jnimble_user (
    id varchar(64) primary key,
    username varchar(255) not null unique,
    password_hash varchar(255) not null,
    display_name varchar(255) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_jnimble_user_status
    on jnimble_user(status);
