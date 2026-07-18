create table if not exists jnimble_audit_log (
    id varchar(64) primary key,
    actor varchar(255) not null,
    action varchar(255) not null,
    target_type varchar(128) not null,
    target_id varchar(255),
    outcome varchar(32) not null,
    message varchar(2000),
    occurred_at timestamp not null
);

create index idx_jnimble_audit_log_occurred_at
    on jnimble_audit_log(occurred_at);

create index idx_jnimble_audit_log_actor
    on jnimble_audit_log(actor);

create index idx_jnimble_audit_log_target
    on jnimble_audit_log(target_type, target_id);
