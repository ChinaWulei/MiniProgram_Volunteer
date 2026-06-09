use volunteer_platform;

create table if not exists activity_priority_rule (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    rule_type varchar(30) not null,
    rule_value varchar(200) not null,
    weight int not null default 10,
    created_at datetime not null default current_timestamp,
    index idx_priority_rule_activity(activity_id)
);
