set names utf8mb4;
use volunteer_service;

alter table user
    add column nickname varchar(50) null after name,
    add column avatar_url varchar(500) null after nickname;

alter table volunteer_profile
    add column bio varchar(500) null after available_time;

alter table activity
    add column signup_requirement varchar(500) null after description,
    add column contact_name varchar(50) null after signup_requirement,
    add column contact_phone varchar(20) null after contact_name,
    add column service_hours decimal(6,2) null after contact_phone,
    add column review_method varchar(50) not null default '人工审核' after service_hours;

update user set nickname = name where nickname is null;
