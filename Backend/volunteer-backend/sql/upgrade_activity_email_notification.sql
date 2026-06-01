alter table user_activity_subscription
    add column wechat_enabled tinyint(1) not null default 1 after enabled,
    add column email_enabled tinyint(1) not null default 0 after wechat_enabled,
    add column email varchar(120) null after email_enabled;
