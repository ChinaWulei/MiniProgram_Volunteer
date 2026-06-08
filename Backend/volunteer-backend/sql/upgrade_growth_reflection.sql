set names utf8mb4;
use volunteer_service;

create table if not exists volunteer_growth_reflection (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    user_id bigint not null,
    content varchar(1200) not null,
    anonymous tinyint(1) not null default 0,
    parsed_gain varchar(500),
    parsed_ability varchar(500),
    parsed_experience varchar(500),
    parsed_advice varchar(500),
    analysis_status varchar(20) not null default 'PENDING',
    recommended tinyint(1) not null default 0 comment '是否允许在活动详情页展示',
    recommended_by bigint comment '审核管理员',
    recommended_at datetime comment '展示状态更新时间',
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_growth_activity_user(activity_id, user_id),
    key idx_growth_recommended(activity_id, recommended),
    constraint fk_growth_activity foreign key(activity_id) references activity(id) on delete cascade,
    constraint fk_growth_user foreign key(user_id) references user(id) on delete cascade,
    constraint fk_growth_admin foreign key(recommended_by) references user(id)
) comment='志愿者活动成长感悟';
