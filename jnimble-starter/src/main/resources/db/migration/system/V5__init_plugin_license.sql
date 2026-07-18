create table if not exists jnimble_plugin_license (
    plugin_id varchar(128) primary key,
    license_id varchar(64) not null,
    token text not null,
    token_hash varchar(64) not null,
    issuer varchar(128) not null,
    key_id varchar(128) not null,
    product_code varchar(128) not null,
    machine_code varchar(255) not null,
    issued_at timestamp not null,
    not_before timestamp not null,
    expires_at timestamp not null,
    time_snapshot text not null,
    snapshot_sequence bigint not null,
    status varchar(64) not null,
    failure_code varchar(128),
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_jnimble_plugin_license_status
    on jnimble_plugin_license(status);

create index idx_jnimble_plugin_license_expires_at
    on jnimble_plugin_license(expires_at);

create table if not exists jnimble_plugin_license_event (
    id bigint primary key auto_increment,
    plugin_id varchar(128) not null,
    action varchar(64) not null,
    status varchar(64) not null,
    detail varchar(2000),
    occurred_at timestamp not null
);

create index idx_jnimble_plugin_license_event_plugin
    on jnimble_plugin_license_event(plugin_id, occurred_at);
