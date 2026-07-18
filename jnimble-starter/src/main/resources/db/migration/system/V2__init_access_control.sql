create table if not exists jnimble_role (
    id varchar(64) primary key,
    code varchar(128) not null unique,
    name varchar(255) not null,
    status varchar(32) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists jnimble_permission (
    code varchar(255) primary key,
    plugin_id varchar(128) not null,
    name varchar(255),
    name_key varchar(255),
    description varchar(1000),
    description_key varchar(255),
    status varchar(32) not null,
    updated_at timestamp not null
);

create index idx_jnimble_permission_plugin_id
    on jnimble_permission(plugin_id);

create index idx_jnimble_permission_status
    on jnimble_permission(status);

create table if not exists jnimble_role_permission (
    role_id varchar(64) not null,
    permission_code varchar(255) not null,
    status varchar(32) not null,
    granted_at timestamp not null,
    updated_at timestamp not null,
    primary key (role_id, permission_code)
);

create index idx_jnimble_role_permission_code
    on jnimble_role_permission(permission_code);

create index idx_jnimble_role_permission_status
    on jnimble_role_permission(status);

create table if not exists jnimble_subject_role (
    subject_id varchar(255) not null,
    role_id varchar(64) not null,
    granted_at timestamp not null,
    primary key (subject_id, role_id)
);

create index idx_jnimble_subject_role_role_id
    on jnimble_subject_role(role_id);
