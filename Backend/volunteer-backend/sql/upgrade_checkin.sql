set names utf8mb4;
use volunteer_service;

alter table activity
    add column latitude decimal(10,6) null after location,
    add column longitude decimal(10,6) null after latitude;

create table if not exists activity_checkin (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    user_id bigint not null,
    status varchar(30) not null,
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
    unique key uk_activity_checkin_user(activity_id, user_id)
);
