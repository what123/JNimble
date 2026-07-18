create table if not exists jnimble_plugin_state (
    plugin_id varchar(128) primary key,
    name varchar(255) not null,
    version varchar(64) not null,
    source varchar(32) not null,
    artifact_path varchar(1024),
    enabled boolean not null default false,
    status varchar(64) not null,
    installed_at timestamp,
    last_started_at timestamp,
    last_stopped_at timestamp,
    last_error varchar(4000),
    descriptor_json text not null,
    descriptor_hash varchar(64) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_jnimble_plugin_state_status
    on jnimble_plugin_state(status);

create index idx_jnimble_plugin_state_enabled
    on jnimble_plugin_state(enabled);
