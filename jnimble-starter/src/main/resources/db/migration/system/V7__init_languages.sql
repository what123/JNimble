create table if not exists jnimble_language (
    language_code varchar(32) primary key,
    locale_tag varchar(64) not null unique,
    name varchar(100) not null,
    native_name varchar(100) not null,
    enabled boolean not null,
    default_language boolean not null,
    sort_order int not null,
    created_at timestamp not null,
    updated_at timestamp not null
);

create index idx_jnimble_language_selector
    on jnimble_language(enabled, sort_order);

insert into jnimble_language (
    language_code, locale_tag, name, native_name, enabled, default_language,
    sort_order, created_at, updated_at
) values (
    'zh_CN', 'zh-CN', 'Chinese (Simplified)', '简体中文', true, true,
    10, current_timestamp, current_timestamp
);

insert into jnimble_language (
    language_code, locale_tag, name, native_name, enabled, default_language,
    sort_order, created_at, updated_at
) values (
    'en_US', 'en-US', 'English', 'English', true, false,
    20, current_timestamp, current_timestamp
);
