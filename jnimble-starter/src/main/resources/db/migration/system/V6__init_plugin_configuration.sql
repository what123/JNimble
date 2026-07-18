create table if not exists jnimble_plugin_configuration (
    plugin_id varchar(128) not null,
    config_key varchar(128) not null,
    config_value text not null,
    encrypted boolean not null default false,
    updated_by varchar(255) not null,
    updated_at timestamp not null,
    primary key (plugin_id, config_key)
);

create index idx_jnimble_plugin_configuration_updated_at
    on jnimble_plugin_configuration(updated_at);
