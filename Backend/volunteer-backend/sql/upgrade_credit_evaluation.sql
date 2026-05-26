set names utf8mb4;
use volunteer_service;

create table if not exists credit_rule (
    id bigint primary key auto_increment,
    code varchar(50) not null unique,
    name varchar(80) not null,
    change_value int not null,
    enabled tinyint(1) not null default 1,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp
) comment='信用分加减规则';

create table if not exists credit_record (
    id bigint primary key auto_increment,
    user_id bigint not null,
    change_value int not null,
    reason varchar(160) not null,
    source_type varchar(40),
    source_id bigint,
    created_at datetime not null default current_timestamp,
    constraint fk_credit_record_user foreign key(user_id) references user(id) on delete cascade
) comment='信用分变更记录';

create table if not exists activity_evaluation (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    evaluator_id bigint not null,
    target_user_id bigint,
    target_type varchar(30) not null comment 'ACTIVITY/LEADER/VOLUNTEER',
    score int not null,
    content varchar(500),
    created_at datetime not null default current_timestamp,
    unique key uk_activity_eval(activity_id,evaluator_id,target_type,target_user_id),
    constraint fk_eval_activity foreign key(activity_id) references activity(id) on delete cascade,
    constraint fk_eval_evaluator foreign key(evaluator_id) references user(id) on delete cascade,
    constraint fk_eval_target_user foreign key(target_user_id) references user(id) on delete cascade
) comment='活动互评';

insert into credit_rule(code,name,change_value,enabled) values
('LEADER_GOOD_REVIEW','负责人五星评价',2,1),
('LEADER_BAD_REVIEW','负责人低分评价',-5,1),
('ABSENT','活动缺勤',-10,1),
('MANUAL_RECOVER','管理员修复信用',5,1)
on duplicate key update name=values(name),change_value=values(change_value),enabled=values(enabled);
