alter table user
    add column openid varchar(80) null after avatar_url,
    add unique key uk_user_openid(openid);

create table if not exists user_activity_subscription (
    id bigint primary key auto_increment,
    user_id bigint not null,
    category varchar(50) not null,
    enabled tinyint(1) not null default 1,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_user_category(user_id, category),
    constraint fk_subscription_user foreign key(user_id) references user(id) on delete cascade
) comment='用户活动类型订阅';
