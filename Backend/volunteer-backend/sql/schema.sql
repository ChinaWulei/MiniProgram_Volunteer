set names utf8mb4;

create database if not exists volunteer_service default character set utf8mb4 collate utf8mb4_general_ci;
use volunteer_service;

drop table if exists service_record;
drop table if exists activity_checkin;
drop table if exists registration;
drop table if exists chat_message;
drop table if exists chat_conversation;
drop table if exists notification;
drop table if exists activity_news_image;
drop table if exists activity_news;
drop table if exists activity;
drop table if exists volunteer_profile;
drop table if exists user;

create table user (
    id bigint primary key auto_increment,
    username varchar(50) not null unique comment '登录账号',
    password varchar(100) not null comment '演示版明文密码，生产环境需加密',
    name varchar(50) not null,
    nickname varchar(50),
    avatar_url varchar(500),
    identity_no varchar(30) not null comment '学号/工号',
    phone varchar(20),
    role varchar(20) not null comment 'VOLUNTEER/ADMIN',
    created_at datetime not null default current_timestamp
) comment='用户表';

create table volunteer_profile (
    id bigint primary key auto_increment,
    user_id bigint not null unique,
    college varchar(80) not null default '数计学院',
    major_class varchar(100),
    skill_tags varchar(255) comment '逗号分隔，如 摄影,文案,讲解',
    available_time varchar(255) comment '如 周末全天,工作日晚上',
    bio varchar(500),
    total_hours decimal(8,2) not null default 0,
    credit_score int not null default 100,
    service_count int not null default 0,
    constraint fk_profile_user foreign key(user_id) references user(id) on delete cascade
) comment='志愿者人才信息库';

create table activity (
    id bigint primary key auto_increment,
    name varchar(120) not null,
    cover_image_url varchar(500),
    category varchar(50) not null,
    location varchar(120) not null,
    latitude decimal(10,6),
    longitude decimal(10,6),
    start_time datetime not null,
    end_time datetime not null,
    signup_start_time datetime,
    signup_deadline datetime,
    checkin_start_time datetime,
    checkin_end_time datetime,
    recruit_number int not null,
    registered_number int not null default 0,
    skill_requirements varchar(255),
    description text,
    signup_requirement varchar(500),
    contact_name varchar(50),
    contact_phone varchar(20),
    service_hours decimal(6,2),
    review_method varchar(50) not null default '人工审核',
    status varchar(20) not null default '报名中' comment '报名中/已满员/已结束',
    created_by bigint,
    finished_at datetime,
    published_at datetime,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    constraint fk_activity_creator foreign key(created_by) references user(id)
) comment='志愿活动表';

create table activity_news (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    title varchar(160) not null,
    content text,
    result_summary varchar(500),
    status varchar(20) not null default 'DRAFT',
    read_count int not null default 0,
    created_by bigint not null,
    published_at datetime,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    constraint fk_news_activity foreign key(activity_id) references activity(id) on delete cascade,
    constraint fk_news_creator foreign key(created_by) references user(id)
) comment='活动新闻帖子';

create table activity_news_image (
    id bigint primary key auto_increment,
    news_id bigint not null,
    image_url varchar(500) not null,
    sort_order int not null default 0,
    created_at datetime not null default current_timestamp,
    constraint fk_news_image_news foreign key(news_id) references activity_news(id) on delete cascade
) comment='活动新闻图片';

create table registration (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    user_id bigint not null,
    status varchar(20) not null default '待审核' comment '待审核/已通过/已拒绝/已完成',
    review_remark varchar(255),
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_activity_user(activity_id, user_id),
    constraint fk_reg_activity foreign key(activity_id) references activity(id) on delete cascade,
    constraint fk_reg_user foreign key(user_id) references user(id) on delete cascade
) comment='活动报名表';

create table service_record (
    id bigint primary key auto_increment,
    user_id bigint not null,
    activity_id bigint not null,
    hours decimal(8,2) not null,
    comment varchar(255),
    created_at datetime not null default current_timestamp,
    constraint fk_record_user foreign key(user_id) references user(id) on delete cascade,
    constraint fk_record_activity foreign key(activity_id) references activity(id) on delete cascade
) comment='志愿服务记录表';

create table activity_checkin (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    user_id bigint not null,
    status varchar(30) not null comment 'CHECKED_IN/LATE_CHECKED_IN/MANUAL_CHECKED_IN/ABSENT',
    checkin_time datetime,
    method varchar(30),
    latitude decimal(10,6),
    longitude decimal(10,6),
    distance_meters decimal(10,2),
    manual_admin_id bigint,
    manual_time datetime,
    manual_reason varchar(255),
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_activity_checkin_user(activity_id, user_id),
    constraint fk_checkin_activity foreign key(activity_id) references activity(id) on delete cascade,
    constraint fk_checkin_user foreign key(user_id) references user(id) on delete cascade,
    constraint fk_checkin_admin foreign key(manual_admin_id) references user(id)
) comment='活动签到与补签记录';

create table chat_conversation (
    id bigint primary key auto_increment,
    user_a_id bigint not null,
    user_b_id bigint,
    type varchar(30) not null default 'PRIVATE' comment 'PRIVATE/SYSTEM',
    last_message varchar(500),
    last_message_at datetime,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_private_pair(user_a_id, user_b_id, type),
    constraint fk_chat_a foreign key(user_a_id) references user(id) on delete cascade,
    constraint fk_chat_b foreign key(user_b_id) references user(id) on delete cascade
) comment='学院内部私聊会话';

create table chat_message (
    id bigint primary key auto_increment,
    conversation_id bigint not null,
    sender_id bigint not null,
    receiver_id bigint not null,
    type varchar(30) not null default 'TEXT' comment 'TEXT/ACTIVITY_INVITE/SYSTEM/REGISTRATION_NOTICE',
    content varchar(1000),
    activity_id bigint,
    image_url varchar(500),
    invite_status varchar(20) comment 'PENDING/ACCEPTED/DECLINED',
    read_at datetime,
    created_at datetime not null default current_timestamp,
    constraint fk_msg_conversation foreign key(conversation_id) references chat_conversation(id) on delete cascade,
    constraint fk_msg_sender foreign key(sender_id) references user(id) on delete cascade,
    constraint fk_msg_receiver foreign key(receiver_id) references user(id) on delete cascade,
    constraint fk_msg_activity foreign key(activity_id) references activity(id) on delete set null
) comment='聊天与邀请消息';

create table notification (
    id bigint primary key auto_increment,
    user_id bigint not null,
    type varchar(40) not null,
    title varchar(120) not null,
    content varchar(500),
    target_type varchar(40),
    target_id bigint,
    read_at datetime,
    created_at datetime not null default current_timestamp,
    constraint fk_notice_user foreign key(user_id) references user(id) on delete cascade
) comment='系统与报名审核通知';
