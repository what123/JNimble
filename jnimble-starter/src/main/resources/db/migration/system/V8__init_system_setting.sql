create table if not exists jnimble_system_setting (
    setting_key   varchar(128) primary key,
    setting_value text         not null,
    updated_by    varchar(255) not null,
    updated_at    timestamp    not null
);

insert into jnimble_system_setting (setting_key, setting_value, updated_by, updated_at) values
    ('site.name', 'JNimble', 'system', current_timestamp),
    ('site.subtitle', 'Operations Console', 'system', current_timestamp),
    ('site.logoUrl', '', 'system', current_timestamp)
on duplicate key update setting_key = setting_key;
